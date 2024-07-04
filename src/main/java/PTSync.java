import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;

/**
 * The {@code PTSync} class provides a method to synchronize MySQL master and slave databases
 * using pt-table-sync within a Docker container.
 */
public class PTSync {

    /**
     * Executes the pt-table-sync command in a Docker container to synchronize
     * the slave database with the master database.
     * conatinerId is your master conatiner name.
     * command that is your want to execute on conatiner bash.
     * @see PrivateDockerConfig
     */
    static void execute(PrivateDockerConfig dockerConfig, String containerId, String command) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerConfig.dockerHost())
                .withDockerTlsVerify(false)
                .withRegistryUsername(dockerConfig.dockerName())
                .withRegistryPassword(dockerConfig.dockerPassword())
                .withRegistryEmail(dockerConfig.dockerEmail())
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        try {
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(true)
                    .withCmd("bash", "-c", command)
                    .exec();

            dockerClient.execStartCmd(execCreateCmdResponse.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err) {
                        @Override
                        public void onNext(Frame item) {
                            if (item.getStreamType() == StreamType.STDOUT) {
                                System.out.print(new String(item.getPayload()));
                            } else if (item.getStreamType() == StreamType.STDERR) {
                                System.err.print(new String(item.getPayload()));
                            }
                        }
                    }).awaitCompletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
