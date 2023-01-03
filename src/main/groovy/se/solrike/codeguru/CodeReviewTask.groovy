package se.solrike.codeguru

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

import software.amazon.awssdk.services.codegurureviewer.CodeGuruReviewerClient
import software.amazon.awssdk.services.codegurureviewer.model.AnalysisType
import software.amazon.awssdk.services.codegurureviewer.model.CreateCodeReviewResponse
import software.amazon.awssdk.services.codegurureviewer.paginators.ListRecommendationsIterable

/**
 * Task to perform an AWS CodeGuru review.
 * <p>
 * The task will use the default credentials and region lookup chain.
 *
 * @author Lucas Persson
 */
abstract class CodeReviewTask extends DefaultTask {


  /**
   * Branch name in git. E.g. 'main'.
   */
  @Input
  public abstract Property<String> getBranchName()

  /**
   * Code review name. Will be appended with a UUID since it needs to be unique.
   */
  @Input
  public abstract Property<String> getCodeReviewName()

  /**
   * ARN for the association to the git repository. E.g.
   * 'arn:aws:codeguru-reviewer:eu-north-1:123345789000:association:47114711-4711-4711-4711-471147114711'
   */
  @Input
  public abstract Property<String> getRepositoryAssociationArn()

  @Internal
  CodeGuruReviewerClient client


  @TaskAction
  void execute() {
    client = CodeGuruReviewerClient.builder().build()
    String codeReviewArn = createCodeReview()
    generateReport(codeReviewArn)
  }

  String createCodeReview() {
    CreateCodeReviewResponse response = client.createCodeReview {
      it.type {
        it.analysisTypes(AnalysisType.CODE_QUALITY, AnalysisType.SECURITY)
        it.repositoryAnalysis {
          it.repositoryHead{
            it.branchName(getBranchName().get())
          }
        }
      }
      it.name(getCodeReviewName().get() + UUID.randomUUID())
      it.repositoryAssociationArn(getRepositoryAssociationArn().get())
    }

    println "Code review status: ${response.codeReview().stateAsString()}"
    println "It takes 5-10 minutes to complete the review. Review: ${response.codeReview().name()}"
    String codeReviewArn = response.codeReview().codeReviewArn()
    client.waiter().waitUntilCodeReviewCompleted {
      it.codeReviewArn(codeReviewArn)
    }
    println "Code review done."
    return codeReviewArn
  }

  void generateReport(String codeReviewArn) {
    ListRecommendationsIterable iterable = client.listRecommendationsPaginator {
      it.codeReviewArn(codeReviewArn)
    }
    iterable.forEach {
      it.recommendationSummaries().forEach {
        println "\n${sIssueTypeIcon[it.recommendationCategoryAsString()]} ${sIssueSeverityIcon[it.severity]} at: $it.filePath:$it.startLine"
        // description is some kind of markdown text
        println it.description()
      }
    }
  }

  // @formatter:off
  private static final Map<String, String> sIssueTypeIcon = [
    'AWSCloudFormationIssues' : '\uD83D\uDE31 Bug  ', // FACE SCREAMING IN FEAR
    'ConcurrencyIssues'       : '\uD83D\uDE31 Bug  ', // FACE SCREAMING IN FEAR
    'ResourceLeaks'           : '\uD83D\uDE31 Bug  ', // FACE SCREAMING IN FEAR
    'CodeMaintenanceIssues'   : '\uD83E\uDD22 Smell', // NAUSEATED FACE
    'CodeInconsistencies'     : '\uD83E\uDD22 Smell', // NAUSEATED FACE
    'AWSBestPractices'        : '\uD83E\uDD22 Smell', // NAUSEATED FACE
    'InputValidations'        : '\uD83E\uDD22 Smell', // NAUSEATED FACE
    'JavaBestPractices'       : '\uD83E\uDD22 Smell', // NAUSEATED FACE
    'SecurityIssues'          : '\uD83D\uDE08 Sec. '  // SMILING FACE WITH HORNS
  ]
  // @formatter:on


  // @formatter:off
  private static final Map<String, String> sIssueSeverityIcon = [
    'Critical': '\uD83C\uDF2A  Crit. ', // Cloud With Tornado
    'High'    : '\uD83C\uDF29  High  ', // Cloud With Lightning
    'Medium'  : '\uD83C\uDF28  Medium', // Cloud With Snow
    'Low'     : '\uD83C\uDF26  Low   ', // White Sun Behind Cloud With Rain
    'Info'    : '\uD83C\uDF24  Info  '  // White Sun With Small Cloud
  ]
  // @formatter:on
}
