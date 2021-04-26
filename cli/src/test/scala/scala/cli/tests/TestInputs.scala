package scala.cli.tests

import java.nio.charset.StandardCharsets

import scala.cli.Inputs
import scala.util.control.NonFatal
import scala.util.Properties

final case class TestInputs(
  files: Seq[(os.RelPath, String)],
  inputArgs: Seq[String] = Nil
) {
  def withInputs[T](f: (os.Path, Inputs) => T): T =
    TestInputs.withTmpDir("scala-cli-tests-") { tmpDir =>
      for ((relPath, content) <- files) {
        val path = tmpDir / relPath
        os.write(path, content.getBytes(StandardCharsets.UTF_8), createFolders = true)
      }

      val inputArgs0 = if (inputArgs.isEmpty) files.map(_._1.toString) else inputArgs
      Inputs(inputArgs0, tmpDir) match {
        case Left(err) => sys.error(err)
        case Right(inputs) => f(tmpDir, inputs)
      }
    }
}

object TestInputs {

  private def withTmpDir[T](prefix: String)(f: os.Path => T): T = {
    val tmpDir = os.temp.dir(prefix = prefix)
    try f(tmpDir)
    finally {
      try os.remove.all(tmpDir)
      catch {
        case _: java.nio.file.FileSystemException if Properties.isWin =>
          Runtime.getRuntime.addShutdownHook(
            new Thread("remove-tmp-dir-windows") {
              setDaemon(true)
              override def run() =
                try os.remove.all(tmpDir)
                catch {
                  case NonFatal(e) =>
                    System.err.println(s"Could not remove temporary directory $tmpDir, ignoring it.")
                }
            }
          )
      }
    }
  }
}
