package de.lhind.u588111.selenium.todo

import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Path
import java.time.Duration

fun main(args: Array<String>) {
    // The selenium server might not be ready yet, so we retry a couple of times.
    // You'd think this would be builtin, but I couldn't find it.
    val page = retry(List(20) { Duration.ofSeconds(5) }) {
        RemoteWebDriver(
            URL("http://selenium-firefox:4444"),
            FirefoxOptions()
                .setBrowserVersion("110.0")
                .addArguments("-headless")
        )
    }

    println("Page created.")

    val wait = WebDriverWait(page, Duration.ofSeconds(2))
    println("Wait created.")

    try {
        page.get("http://frontend:8080")
        println("Navigated to test site")

        wait.until { page.deleteButton?.isDisplayed }

        page.screenshot("01.Start.png")
        println("Created Screenhot")

        check(page.checkboxSequence.none()) { "This test assumes an empty todo list." }

        // Add todos
        (1..5).forEach {
            page.taskInput.sendKeys("Todo $it")
            page.addButton.click()
        }
        println("Added Todos")

        page.screenshot("02.Items.png")

        // Clear items one-by-one
        page.checkboxSequence.forEach {
            it.click()
            page.deleteButton.click()
            wait.until(stalenessOf(it))
        }
        println("Checked off todos")

        page.screenshot("03.Done.png")
        println("Created third screenshot")
    } catch (e: Exception) {
        page.screenshot("XX.${e.javaClass.simpleName}.png")
        println("Created error screenshot")
        error(e.localizedMessage)
    } finally {
        page.close()
    }

}

// We access everything lazily so that we don't run into stale element errors
val WebDriver.checkboxSequence: Sequence<WebElement>
    get() = generateSequence {
        runCatching { findElement(By.className("todoCheckbox")) }.getOrNull()
    }
val WebDriver.taskInput get() = findElement(By.id("taskInput"))
val WebDriver.addButton get() = findElement(By.id("addButton"))
val WebDriver.deleteButton get() = findElement(By.id("deleteButton"))

private fun TakesScreenshot.screenshot(filename: String) {
    getScreenshotAs(OutputType.BYTES).also {
        val file = Path.of("/usr", "local", "images", filename).toFile()
        FileOutputStream(file).write(it)
    }
}

private fun <T> retry(durations: Iterable<Duration>, action: () -> T): T {
    for (duration in durations) {
        try {
            return action()
        } catch (e: Exception) {
            println("Failure: '${e.message}'. Waiting $duration.")
            Thread.sleep(duration.toMillis())
        }
    }
    return action() // One last time. If this throws, we give up.
}

