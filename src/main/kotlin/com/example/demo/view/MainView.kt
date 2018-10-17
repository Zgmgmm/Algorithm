package com.example.demo.view

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.SelectionMode
import tornadofx.*
import java.lang.Thread.sleep

class MainView : View("Hello TornadoFX") {
    override val root = borderpane {
        top<TopView>()
        bottom<BottomView>()
        val btn = lookup("#button") as Button
        val canvas = lookup("#canvas") as Canvas
        btn.setOnAction {
            runAsync {
                val ctx = canvas.graphicsContext2D
                while (true) {
                    sleep(100)
                    ctx.beginPath()
                    val r = Math.random() * 20 + 10
                    ctx.fillOval(Math.random() * 500, Math.random() * 500, r, r)
                    ctx.strokeLine(Math.random() * 500, Math.random() * 500, Math.random() * 500, Math.random() * 500);
                }
            }
        }

    }

}

class TopView : View() {
    override val root = button("Top View") {
        id = "button"
    }
}

class BottomView : View() {
    private lateinit var ctx: GraphicsContext
    override val root = vbox {
        canvas(1000.0, 1000.0) {
            id = "canvas"
            ctx = graphicsContext2D
        }
    }

}


class ListTest : View() {
    override val root = vbox {
        listview<String> {
            items.add("Alpha")
            items.add("Beta")
            items.add("Gamma")
            items.add("Delta")
            items.add("Epsilon")
            selectionModel.selectionMode = SelectionMode.MULTIPLE
        }
    }
}