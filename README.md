# codeguru-gradle-plugin
Gradle plugin for performing AWS CodeGuru reviews (static code analysis).

See [this blog](https://www.linkedin.com/pulse/aws-codeguru-static-code-analysis-lucas-persson/) for
a quick intro to the tool.


# Usage

## Apply to your project

Apply the plugin to your project.

```groovy
plugins {
  id 'se.solrike.codeguru' version '1.0.0'
}
```
Gradle 7.0 or later must be used.


## Quick start

### Pre-requisites in AWS
Associate AWS CodeGuru with your repository at GitHub by first allow AWS to access your GitHub account
and then pick your repositories that you want to review.

Take note of the ARN for the repository association since the task needs to be configured with that.

### In build.gradle

```groovy
plugins {
  id 'se.solrike.codeguru' version '1.0.0'
}

task codeReview(type: se.solrike.codeguru.CodeReviewTask) {
  description = 'AWS GodeGuru review. Default AWS credentials provider chain is used.'
  branchName = 'main'
  codeReviewName = 'my-review-123'
  repositoryAssociationArn = 'arn:aws:codeguru-reviewer:eu-north-1:123456789000:association:47114711-4711-4711-4711-4711471117'
}
```

### Execute
Configure your AWS credentials, for instance as environment variables.
Run the task to do the code review. It takes about 5-10 minutes to complete.

```
./gradlew codeReview
```


### The report
Sample report looks like this:

```
🤢 Smell 🌨  Medium at: src/main/java/se/solrike/springawsextras/context/ResourceMessageSource.java:71
**Problem**: While wrapping the caught exception into a custom one, information about the caught exception is being lost, including information about the stack trace of the exception.

**Fix**: If the caught exception object does not contain sensitive information, consider passing it as the "rootCause" or inner exception parameter to the constructor of the new exception before throwing the new exception. (Note that not all exception constructors support inner exceptions. Use a wrapper exception that supports inner exceptions.)
[Learn more](https://www.ibm.com/support/pages/best-practice-catching-and-re-throwing-java-exceptions)
Similar issue at line numbers 77.

```

# Release notes
### 1.0.0
Task to perform code review which generates a report on the console in plain text.
