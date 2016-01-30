package example

import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html._
import example.ScalaJSCode._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import shared.SharedCode._
import shared.UltraRapidImage
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

object UltraRapid2Test {

  private val testQuestionAmount = 27
  private var backend: scala.Option[Backend2] = None
  private val question = getElementById[Div]("ultra-rapid-2")
  private var interval: js.UndefOr[js.timers.SetIntervalHandle] =
    js.undefined

  private def getBackend(sc: BackendScope[_, State]): Backend2 = {
    backend match {
      case None => backend = Some(new Backend2(sc, true, None))
      case Some(x) => {
        val b = new Backend2(sc, true, Some(x.report.get))
        backend = Some(b)
      }
    }
    backend.get
  }

  def constructArrayBuffer(s: String) = {
    val bigDiv = dom.document.getElementById("big").asInstanceOf[Div]
    val res = new ArrayBuffer[UltraRapidImage]()
    val pairs = s.split(";")
    val div = dom.document.createElement("div").asInstanceOf[Div]
    div.setAttribute("hidden", "true")
    div.id = "preload-div"
    getElementById[Body]("body").appendChild(div)
    var loaded = 0
    var size = 0
    for (pair <- pairs) {
      size += 1
      val image = UltraRapidImage(pair.split(",")(0), pair.split(",")(1), false)
      res.append(image)
      val newChild = dom.document.createElement("img").asInstanceOf[Image]
      newChild.src = "/assets/images/test2/open_experiment/" + image.imageName + ".jpg"
      // TODO wait till 0.8.2 release
      newChild.addEventListener("load", { e: Event => {
        loaded += 1
        image.preloaded = true
      }
      })
      getElementById[Div]("preload-div").appendChild(newChild)
    }
    var preloadInterval: SetIntervalHandle = null
    preloadInterval = js.timers.setInterval(500)({
      getElementById[Div]("loading-bar").style.width = s"${(100 * loaded) / size}%"
      if (loaded == size) {
        bigDiv.setAttribute("hidden", "false")
        js.timers.clearInterval(preloadInterval)
      }
    })
    val cross = dom.document.createElement("img").asInstanceOf[Image]
    cross.src = "/assets/images/cross.png"
    getElementById[Div]("preload-div").appendChild(cross)
    util.Random.shuffle(res)
  }

  private val strings = constructArrayBuffer(getElementById[Div]("images").getAttribute("data-images"))

  def customP(innerText: String): ReactElement = {
    div(
      p(
        innerText,
        position := "absolute",
        top := "50%",
        left := "50%",
        marginRight := "-50%",
        transform := "translate(-50%, -50%)"
      )
    )
  }

  def getRandomQuestion(images: ArrayBuffer[UltraRapidImage]): (UltraRapidImage, ArrayBuffer[UltraRapidImage]) = {
    var idx = generateRandomIndex(images.length)
    while (!images(idx).preloaded) {
      idx = generateRandomIndex(images.length)
    }
    val img = images.remove(idx)
    (img, images)
  }

  val buttonApp = ReactComponentB[Unit]("StartButton")
    .initialState("")
    .backend(new TestBackend(_))
    .render((_, S, B) => button(
    `class` := "btn btn-primary",
    onClick ==> B.startTest,
    "Start test!"
  )
    )
    .buildU

  class TestBackend($: BackendScope[_, String]) {

    def startTest(e: ReactEventI) = {

      val questionTypes = new ArrayBuffer[Int]()
      questionTypes.append(0)
      val testApp = ReactComponentB[Unit]("TestSession")
        .initialState(StateObj.apply(getRandomQuestion(strings), FixationCross(500, false), true,
        1, testQuestionAmount, true))
        .backend(sc => getBackend(sc))
        .render((_, S, B) => {
        S.whatToShow match {
          case FixationCross(_, _) => img(src := "/assets/images/cross.png", marginLeft := "auto", marginRight := "auto", display := "block")
          case CorrectAnswerCross(_, _) => img(src := "/assets/images/cross-correct.png", marginLeft := "auto", marginRight := "auto", display := "block")
          case IncorrectAnswerCross(_, _) => img(src := "/assets/images/cross-incorrect.png", marginLeft := "auto", marginRight := "auto", display := "block")
          case ImageQuestion(_, _) => img(src := "/assets/images/test2/open_experiment/" + S.res._1.imageName + ".jpg", marginLeft := "auto", marginRight := "auto", display := "block")
          case TextQuestion(_, _) => {
            B.clicked = false
            dom.document.onmousedown = {
              (e: dom.MouseEvent) =>
                if (S.whatToShow.isInstanceOf[TextQuestion]) {
                  B.clicked = true
                  B.showPicture(questionTypes, testQuestionAmount)
                }
            }
            div()
            //              askQuestion(S.questionType)
          }
          case NoNextState(_) => {
            // todo wait for click on button
            div(
              h4("Question := " + S.questionType),
              textarea("Enter your answer", rows := 10, cols := 70, `class` := "form-control"),
              button("Yes", `class` := "btn btn-primary", onClick ==> B.nextImage)
            )
          }
          case Rest(_, _) => {
            // reduce number of questions to be asked for this type of a question
            dom.document.onkeypress = {
              (e: dom.KeyboardEvent) => {}
            }
            h1()

          }
        }
        //        else {
        //          js.timers.clearInterval(B.interval.get)
        //          val paragraph = getElementById[Paragraph]("countdown")
        //          paragraph.textContent = "Осталось: 120 секунд"
        ////          var cnt = 120
        ////          interval = js.timers.setInterval(1000)({
        ////            if (cnt < 0) {
        ////              paragraph.textContent = "_____"
        ////              clearInterval
        ////              React.render(realTestApp(), question)
        ////            } else {
        ////              paragraph.textContent = "Осталось: " + cnt + " секунд"
        ////              cnt -= 1
        ////            }
        ////          })
        //
        //          h4("Внимание! " +
        //            "Начинается основная серия эксперимента. Напоминаем инструкцию. Вы увидите фиксационный крест, " +
        //            "и после него на доли секунды появится изображение-картинка, которая быстро исчезнет. ", br, br,
        //            "После этого на экране появится вопрос о содержании этого изображения (пример вопроса: Это изображение природы?) ", br, br,
        //            "Если Ваш ответ на данный вопрос положительный – нажмите «пробел» сразу после появления вопроса, " +
        //              "если Ваш ответ отрицательный – дождитесь следующего задания, а именно появления фиксационного креста.", br, br,
        //            "Эксперимент начнется через 2 минуты")
        //
        //        }
      })
        .componentDidMount(f => {
        f.backend.init(f.state, questionTypes, testQuestionAmount)
      })
        .buildU
      React.render(testApp(), question)
      getElementById[Div]("instruction").innerHTML = "<p id=\"countdown\">_____</p>"
      $.setState("")
    }
  }

  def doTest() = {
    React.render(buttonApp.apply(), question)
  }

  def clearInterval = {
    js.timers.clearInterval(interval.get)
  }


}
