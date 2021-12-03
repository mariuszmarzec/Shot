package com.karumi.shot.tasks

import com.android.builder.model.{BuildType, ProductFlavor}
import com.karumi.shot.android.Adb
import com.karumi.shot.base64.Base64Encoder
import com.karumi.shot.reports.{ConsoleReporter, ExecutionReporter}
import com.karumi.shot.screenshots.{
  ScreenshotsComparator,
  ScreenshotsDiffGenerator,
  ScreenshotsSaver
}
import com.karumi.shot.system.EnvVars
import com.karumi.shot.ui.Console
import com.karumi.shot.{Files, Shot, ShotExtension}
import org.gradle.api.{DefaultTask, GradleException}
import org.gradle.api.tasks.TaskAction

abstract class ShotTask extends DefaultTask {
  var appId: String        = _
  var flavor: String       = _
  var buildType: BuildType = _
  private val console      = new Console
  protected val shot: Shot =
    new Shot(
      new Adb,
      new Files,
      new ScreenshotsComparator,
      new ScreenshotsDiffGenerator(new Base64Encoder),
      new ScreenshotsSaver,
      console,
      new ExecutionReporter,
      new ConsoleReporter(console),
      new EnvVars()
    )
  protected val shotExtension: ShotExtension =
    getProject.getExtensions.findByType(classOf[ShotExtension])

  setGroup("shot")

}

object ExecuteScreenshotTests {
  def name(flavor: String, buildType: BuildType) =
    if (flavor.isEmpty) {
      s"${buildType.getName}ExecuteScreenshotTests"
    } else {
      s"${flavor}${buildType.getName.capitalize}ExecuteScreenshotTests"
    }

  def description(flavor: String, buildType: BuildType) =
    s"Checks the user interface screenshot tests . If you execute this task using -Precord param the screenshot will be regenerated for the build ${flavor.capitalize}${buildType.getName.capitalize}"
}

class ExecuteScreenshotTests extends ShotTask {

  @TaskAction
  def executeScreenshotTests(): Unit = {
    val system = System.getProperty("os.name")
    val project = getProject
    val tolerance = project.getExtensions
      .getByType[ShotExtension](classOf[ShotExtension])
      .tolerance
    val recordScreenshots = project.hasProperty("record")
    val printBase64       = project.hasProperty("printBase64")
    val showOnlyFailingTestsInReports = project.getExtensions
      .getByType[ShotExtension](classOf[ShotExtension])
      .showOnlyFailingTestsInReports
    val projectFolder = project.getProjectDir.getAbsolutePath
    val projectName   = project.getName
    val buildFolder   = project.getBuildDir.getAbsolutePath
    if (recordScreenshots) {
      shot.recordScreenshots(
        system,
        appId,
        buildFolder,
        projectFolder,
        projectName,
        flavor,
        buildType.getName
      )
    } else {
      val result = shot.verifyScreenshots(
        system,
        appId,
        buildFolder,
        projectFolder,
        flavor,
        buildType.getName,
        project.getName,
        printBase64,
        tolerance,
        showOnlyFailingTestsInReports
      )
      if (result.hasErrors) {
        throw new GradleException(
          "Screenshots comparision fail. Review the execution report to see what's broken your build."
        )
      }
    }
  }
}

object DownloadScreenshotsTask {
  def name(flavor: String, buildType: BuildType) =
    if (flavor.isEmpty) {
      s"${buildType.getName}DownloadScreenshots"
    } else {
      s"${flavor}${buildType.getName.capitalize}DownloadScreenshots"
    }

  def description(flavor: String, buildType: BuildType) =
    s"Retrieves the screenshots stored into the Android device where the tests were executed for the build ${flavor.capitalize}${buildType.getName.capitalize}"
}

class DownloadScreenshotsTask extends ShotTask {
  @TaskAction
  def downloadScreenshots(): Unit = {
    val projectFolder = getProject.getProjectDir.getAbsolutePath
    shot.downloadScreenshots(projectFolder, flavor, buildType.getName, appId)
  }
}

object RemoveScreenshotsTask {
  def name(flavor: String, buildType: BuildType, beforeExecution: Boolean) = {
    val suffix = if (beforeExecution) "Before" else "After"
    if (flavor.isEmpty) {
      s"${buildType.getName}RemoveScreenshots${suffix}"
    } else {
      s"${flavor}${buildType.getName.capitalize}RemoveScreenshots${suffix}"
    }
  }

  def description(flavor: String, buildType: BuildType) =
    s"Removes the screenshots recorded during the tests execution from the Android device where the tests were executed for the build ${flavor.capitalize}${buildType.getName.capitalize}"
}

class RemoveScreenshotsTask extends ShotTask {
  @TaskAction
  def clearScreenshots(): Unit =
    shot.removeScreenshots(appId)
}
object ExecuteScreenshotTestsForEveryFlavor {
  val name: String = "executeScreenshotTests"
}
class ExecuteScreenshotTestsForEveryFlavor extends ShotTask {
  setDescription(
    "Checks the user interface screenshot tests. If you execute this task using -Precord param the screenshot will be regenerated."
  )
}
