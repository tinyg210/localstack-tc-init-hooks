package dev.ancaghenade.testcontainersinithooks;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.services.s3.S3Client;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class S3Test {

  private static final Logger LOGGER = LoggerFactory.getLogger(S3Test.class);
  private static Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);

// Failing config
//  @Container
//  protected static final LocalStackContainer localStack =
//      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0")) // works with localstack/localstack:1.4.0
//          .withCopyFileToContainer(
//              MountableFile.forClasspathResource("init.sh"),
//              "/docker-entrypoint-initaws.d/init.sh");

  @Container
  protected static final LocalStackContainer localStack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
          .withCopyToContainer(
              MountableFile.forClasspathResource("init.sh"
              //, 775
              ),
              "/etc/localstack/init/ready.d/init.sh");

  @Autowired
  private S3Client s3Client;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.cloud.aws.s3.endpoint", () -> localStack.getEndpoint().toString());
  }

  @BeforeAll
  static void setup() {
    localStack.followOutput(logConsumer);
  }


  @Test
  public void testS3Created() throws IOException, InterruptedException {
    localStack.execInContainer("awslocal", "s3api", "wait", "bucket-exists", "--bucket",
        "important-files");
    Assertions.assertEquals(1, this.s3Client.listBuckets().buckets().size());
    Assertions.assertEquals("important-files", this.s3Client.listBuckets().buckets().get(0).name());

  }

}