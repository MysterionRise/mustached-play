package example

import org.scalajs.dom
import org.scalajs.dom.html._
import example.ScalaJSCode._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import shared.SharedCode._
import shared.UltraRapidImage
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js

object UltraRapidTest {

  private val topMargin = 200
  private val testQuestionAmount = 1
  private val questionAmount = 1
  private val testQuestionTypes = util.Random.shuffle(ArrayBuffer(1, 2, 3, 4, 5, 6))
  private val questionTypes = util.Random.shuffle(ArrayBuffer(1, 2, 3, 4, 5, 6))
  private val socialTestQuestionTypes = util.Random.shuffle(ArrayBuffer(7, 8))
  private val socialQuestionTypes = util.Random.shuffle(ArrayBuffer(7, 8))
  private val socialTestQuestionAmount = 1
  private val socialQuestionAmount = 1

  /**
   * @param image - current image, that we want to show
   * @param whatToShow - type of showing (fixation cross, question image, text question, rest)
   * @param isTesting - boolean flag representing test session or not
   * @param images - list of images
   * @param questionType - type of question
   *                     1 - is it a dog?
   *                     2 - is it animal?
   *                     3 - is it car?
   *                     4 - is it vehicle?
   *                     5 - is it nature?
   *                     6 - is it urban?
   *                     7 - is it indoor scene?
   *                     8 - is it positive interaction on scene?
   */
  case class State(image: UltraRapidImage, whatToShow: WhatToShow, isTesting: Boolean,
                   images: ArrayBuffer[UltraRapidImage], questionType: Int, numberOfQuestions: Int)


  def constructArrayBuffer(s: String) = {
    val res = new ArrayBuffer[UltraRapidImage]()
    val pairs = s.split(";")
    val div = dom.document.createElement("div").asInstanceOf[Div]
    div.setAttribute("hidden", "true")
    div.id = "preload-div"
    getElementById[Body]("body").appendChild(div)
    for (pair <- pairs) {
      val image = UltraRapidImage(pair.split(",")(0), pair.split(",")(1))
      res.append(image)
      val newChild = dom.document.createElement("img").asInstanceOf[Image]
      newChild.src = "/assets/images/ultraRapid/" + image.imageName + ".jpg"
      getElementById[Div]("preload-div").appendChild(newChild)
    }
    val cross = dom.document.createElement("img").asInstanceOf[Image]
    cross.src = "/assets/images/cross.png"
    getElementById[Div]("preload-div").appendChild(cross)
    val crossGreen = dom.document.createElement("img").asInstanceOf[Image]
    crossGreen.src = "/assets/images/cross-correct.png"
    getElementById[Div]("preload-div").appendChild(crossGreen)
    val crossRed = dom.document.createElement("img").asInstanceOf[Image]
    crossRed.src = "/assets/images/cross-incorrect.png"
    getElementById[Div]("preload-div").appendChild(crossRed)
    util.Random.shuffle(res)
  }

  private val testStrings = constructArrayBuffer(getElementById[Div]("images").getAttribute("data-images"))

  def customP(innerText: String): ReactElement = {
    h2(
      p(
        marginTop := topMargin,
        marginLeft := topMargin,
        innerText
      )
    )
  }

  /**
   * Mapping for targets non-targets in questions
   */
  val mapping = new Array[Set[Int]](10)
  mapping(1) = Set(1, 2, 3)
  mapping(2) = Set(2, 3, 4)
  mapping(3) = Set(1, 3, 4)
  mapping(4) = Set(1, 2, 4)
  mapping(5) = Set(5, 6)
  mapping(6) = Set(5, 6)
  mapping(7) = Set(7)
  mapping(8) = Set(7)

  def getRandomQuestion(images: ArrayBuffer[UltraRapidImage], qType: Int): UltraRapidImage = {
    val s = mapping.apply(qType)
    var idx = generateRandomIndex(images.length)
    var cnt = 0
    // todo fix if we don't have targets or non-targets for this type of a question
    while (!s.contains(Integer.parseInt(images(idx).imageType)) && cnt < images.length) {
      idx = generateRandomIndex(images.length)
      cnt += 1
    }
    images.remove(idx)
  }

  val question = getElementById[Div]("ultra-rapid")

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

    def askQuestion(questionType: Int): ReactElement = {
      questionType match {
        case 1 => customP("Did you see dog here?")
        case 2 => customP("Did you see animal here?")
        case 3 => customP("Did you see car here?")
        case 4 => customP("Did you see vehicle here?")
        case 5 => customP("Did you see nature scene here?")
        case 6 => customP("Did you see urban scene here?")
        case 7 => customP("Did you see indoor scene here?")
        case 8 => customP("Did you see positive interaction here?")
        case _ => p("We don't have any questions for that type!")
      }
    }

    def createSocialApp(testQType: Int) = {
      ReactComponentB[Unit]("RealSocialSession")
        .initialState(State(getRandomQuestion(testStrings, testQType), FixationCross(500), false,
        testStrings, testQType, 0))
        .backend(new Backend(_, true))
        .render((_, S, B) => {
        if (S.questionType > 0) {
          S.whatToShow match {
            case FixationCross(_) => img(src := "/assets/images/cross.png")
            case ImageQuestion(_) => img(src := "/assets/images/ultraRapid/" + S.image.imageName + ".jpg")
            case TextQuestion(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) =>
                  if (e.charCode == 32 && S.whatToShow.isInstanceOf[TextQuestion]) {
                    val user = getElementById[Heading]("user")
                    var userID: String = user.getAttribute("data-user-id")
                    // TODO for testing purposes only
                    if (userID.isEmpty) {
                      userID = "123"
                    }
                    B.notClicked = false
                    B.showPicture(socialQuestionTypes, socialQuestionAmount)
                  }
              }
              askQuestion(S.questionType)
            }
            case Rest(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) => {}
              }
              h1()
            }
          }
        } else {
          js.timers.clearInterval(B.interval.get)
          // TODO finishing tests
          div(
            form(
              action := "/tests/finishTest?report=\"" + B.report.toString + "\"",
              `class` := "form-horizontal",
              method := "POST",
              button(
                id := "finish-test",
                `type` := "submit",
                `class` := "btn btn-primary",
                "Finish test"
              )
            )
          )

        }
      })
        .componentDidMount(f => {
        f.backend.init(f.state, socialQuestionTypes, socialQuestionAmount)
      })
        .buildU
    }

    def createTestSocialApp(testQType: Int) = {
      ReactComponentB[Unit]("TestSocialSession")
        .initialState(State(getRandomQuestion(testStrings, testQType), FixationCross(500), true,
        testStrings, testQType, 0))
        .backend(new Backend(_, true))
        .render((_, S, B) => {
        if (S.questionType > 0) {
          S.whatToShow match {
            case FixationCross(_) => img(src := "/assets/images/cross.png")
            case CorrectAnswerCross(_) => img(src := "/assets/images/cross-correct.png")
            case IncorrectAnswerCross(_) => img(src := "/assets/images/cross-incorrect.png")
            case ImageQuestion(_) => img(src := "/assets/images/ultraRapid/" + S.image.imageName + ".jpg")
            case TextQuestion(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) =>
                  if (e.charCode == 32 && S.whatToShow.isInstanceOf[TextQuestion]) {
                    val user = getElementById[Heading]("user")
                    var userID: String = user.getAttribute("data-user-id")
                    // TODO for testing purposes only
                    if (userID.isEmpty) {
                      userID = "123"
                    }
                    B.notClicked = false
                    B.showPicture(socialTestQuestionTypes, socialTestQuestionAmount)
                  }
              }
              askQuestion(S.questionType)
            }
            case Rest(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) => {}
              }
              h1()
            }
          }
        } else {
          js.timers.clearInterval(B.interval.get)
          // TODO start to ask real social tasks
          val testQType = socialQuestionTypes.remove(0)
          val app = createSocialApp(testQType)
          React.render(app.apply(), question)
        }
      })
        .componentDidMount(f => {
        f.backend.init(f.state, socialTestQuestionTypes, socialTestQuestionAmount)
      })
      .buildU
    }

    def startTest(e: ReactEventI) = {

      val realTestQType = questionTypes.remove(0)
      val realTestApp = ReactComponentB[Unit]("RealSession")
        .initialState(State(getRandomQuestion(testStrings, realTestQType), FixationCross(500), false,
        testStrings, realTestQType, 0))
        .backend(new Backend(_, true))
        .render((_, S, B) => {
        if (S.questionType > 0) {
          S.whatToShow match {
            case FixationCross(_) => img(src := "/assets/images/cross.png")
            case ImageQuestion(_) => img(src := "/assets/images/ultraRapid/" + S.image.imageName + ".jpg")
            case TextQuestion(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) =>
                  if (e.charCode == 32 && S.whatToShow.isInstanceOf[TextQuestion]) {
                    val user = getElementById[Heading]("user")
                    var userID: String = user.getAttribute("data-user-id")
                    // TODO for testing purposes only
                    if (userID.isEmpty) {
                      userID = "123"
                    }
                    B.notClicked = false
                    B.showPicture(questionTypes, questionAmount)
                  }
              }
              askQuestion(S.questionType)
            }
            case Rest(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) => {}
              }
              h1()
            }
          }
        } else {
          js.timers.clearInterval(B.interval.get)
          // TODO start to ask social tasks
          val testQType = socialTestQuestionTypes.remove(0)
          val app = createTestSocialApp(testQType)
          React.render(app.apply(), question)
        }
      })
        .componentDidMount(f => {
        f.backend.init(f.state, questionTypes, questionAmount)
      })
        .buildU

      val testQType = testQuestionTypes.remove(0)
      val testApp = ReactComponentB[Unit]("TestSession")
        .initialState(State(getRandomQuestion(testStrings, testQType), FixationCross(500), true,
        testStrings, testQType, 0))
        .backend(new Backend(_, true))
        .render((_, S, B) => {
        if (S.questionType > 0) {
          S.whatToShow match {
            case FixationCross(_) => img(src := "/assets/images/cross.png")
            case CorrectAnswerCross(_) => img(src := "/assets/images/cross-correct.png")
            case IncorrectAnswerCross(_) => img(src := "/assets/images/cross-incorrect.png")
            case ImageQuestion(_) => img(src := "/assets/images/ultraRapid/" + S.image.imageName + ".jpg")
            case TextQuestion(_) => {
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) =>
                  if (e.charCode == 32 && S.whatToShow.isInstanceOf[TextQuestion]) {
                    val user = getElementById[Heading]("user")
                    var userID: String = user.getAttribute("data-user-id")
                    // TODO for testing purposes only
                    if (userID.isEmpty) {
                      userID = "123"
                    }
                    B.notClicked = false
                    B.showPicture(testQuestionTypes, testQuestionAmount)
                  }
              }
              askQuestion(S.questionType)
            }
            case Rest(_) => {
              // reduce number of questions to be asked for this type of a question
              dom.document.onkeypress = {
                (e: dom.KeyboardEvent) => {}
              }
              h1()
            }
          }
        } else {
          js.timers.clearInterval(B.interval.get)
          // TODO ask to be ready for good testing
          React.render(realTestApp(), question)
        }
      })
        .componentDidMount(f => {
        f.backend.init(f.state, testQuestionTypes, testQuestionAmount)
      })
        .buildU
      React.render(testApp(), question)
      getElementById[Div]("instruction").innerHTML = ""
      $.setState("")
    }
  }

  def doTest() = {
    React.render(buttonApp.apply(), question)
  }

}
