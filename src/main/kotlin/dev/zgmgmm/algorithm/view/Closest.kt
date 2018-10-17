package dev.zgmgmm.algorithm.view

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import tornadofx.*
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CountDownLatch


fun randFLT(min: Double, max: Double) = Math.random() * (max - min) + min
fun Pair<Point, Point>.toLine(color: Color = Color.BLACK) = Line(this.first, this.second, color)
operator fun Point.minus(other: Point) = Line(this, other)
class Closest : View() {
    lateinit var canvas: Canvas
    lateinit var ctx: GraphicsContext
    private var INTERVAL = 200L
    private var LINE_WIDTH = 3.0

    private var MIN_X = 0.0
    private var MAX_X = 800.0
    private var MIN_Y = 0.0
    private var MAX_Y = 800.0

    private val n = 50
    private var minD = Double.POSITIVE_INFINITY
    private var closestPair: Pair<Point, Point>? = null
        set(value) {
            field = value
            minD = value?.first?.distanceTo(value.second) ?: Double.POSITIVE_INFINITY
            repaint()
        }
    private val input = arrayListOf<Point>()
    private var testingPair: Pair<Point, Point>? = null
        set(value) {
            field = value
            repaint()
        }
    private var highlightedPointsStack = object : MyStack<Collection<Point>>() {
        override fun push(item: Collection<Point>): Collection<Point> {
            return super.push(item.map { it.copy(color = Color.YELLOW) })
        }
    }
    private var midXStack = MyStack<Double>()
    private lateinit var stateText: Text
    private var state = ""
        set(value) {
            field = value
            Platform.runLater { stateText.text = field }
            repaint()
        }
    private var pointScanning: Point? = null
        set(value) {
            field = value?.copy(color = Color.RED)
            INTERVAL = if (field != null) 1000 else 200
            repaint()
        }

    private var drawStrip = false
        set(value) {
            field = value
            repaint()
        }
    private var awaitClicking = false
    private var latch = CountDownLatch(1)


    override val root = vbox {
        stateText = text {
            alignment = Pos.CENTER
            textAlignment = TextAlignment.CENTER
            font = Font(20.0)
        }
        canvas = canvas {
            width = MAX_X
            height = MAX_Y
            setOnMouseClicked {
                when (it.button) {
                    MouseButton.SECONDARY -> {
                        awaitClicking = !awaitClicking
                        if (!awaitClicking)
                            latch.countDown()
                        else
                            latch = CountDownLatch(1)
                    }
                    MouseButton.PRIMARY -> {
                        latch.countDown()
                    }
                    else -> {
                    }
                }
            }
        }



        runAsync {
            ctx = canvas.graphicsContext2D
            ctx.lineWidth = LINE_WIDTH

            val p = arrayListOf<Point>()
            for (i in 0 until n) {
                val x = randFLT(MIN_X, MAX_X)
                val y = randFLT(MIN_Y, MAX_Y)
                p.add(Point(x, y))
            }

            p.forEach {
                input += it.copy(color = Color.BLACK)
            }
            repaint()
            closestPair(p, n)
        }

    }


    fun repaint() {
        if (awaitClicking)
            latch.await()
        sleep(INTERVAL)
        clearCanvas()
        if (drawStrip) {
            val midX = midXStack.peek()
            if (midX != null) {
                val left = midX - minD
                val right = midX + minD
                val top = MIN_Y
                val bottom = MAX_Y
                ctx.save()
                ctx.fill = Color.rgb(0, 0, 0, 0.2)
                ctx.fillRect(left, top, right - left, bottom - top)
                ctx.restore()
            }
        }

        val top = pointScanning?.y
        if (top != null) {
            val midX = midXStack.peek()
            val left = midX!! - minD
            val right = midX + minD
            val bottom = top + 2 * minD
            val color = Color.rgb(0, 255, 0, 0.2)
            ctx.save()
            ctx.fill = color
            ctx.fillRect(left, top, right - left, bottom - top)
            ctx.restore()
        }
//        ctx.save()
//        ctx.lineWidth=2.0
//        ctx.font=Font(ctx.font.name,20.0)
//        ctx.strokeText(state,MAX_X/2,50.0)
//        ctx.restore()

        input.forEach {
            drawPoint(it)
        }

        val midX = midXStack.peek()
        if (midX != null) {
            val dividingLine = Line(midX, MIN_Y, midX, MAX_Y, Color.GREY, 5.0)
            drawLine(dividingLine)
        }

        highlightedPointsStack.peek()?.forEach {
            drawPoint(it)
        }
        drawPoint(pointScanning)
        drawLine(closestPair?.toLine(color = Color.RED))
        drawLine(testingPair?.toLine())

        latch = CountDownLatch(1)
    }

    private fun closestPair(p: List<Point>, n: Int) {
        var px = p.toList()
        var py = p.toList()
        py = py.sortedBy { it.y }
        px = px.sortedBy { it.x }
        divideAndConquer(px, py, n)
    }


    private fun bruteForce(p: List<Point>, n: Int) {
        state = "bruteForce"
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                testPair(p[i], p[j])
            }
        }
        state = ""

    }

    private fun testPair(p1: Point, p2: Point) {
        val d = p1.distanceTo(p2)
        testingPair = Pair(p1, p2)
        if (d < minD)
            closestPair = Pair(p1, p2)
        testingPair = null
    }

    private fun divideAndConquer(px: List<Point>, py: List<Point>, n: Int) {
        highlightedPointsStack.push(px)

        if (n < 2) {
            highlightedPointsStack.pop()
            return
        }
        if (n < 4) {
            bruteForce(px, n)
            highlightedPointsStack.pop()
            return
        }



        state = "dividing"
        val mid = px.size / 2
        val midX = px[mid].x
        midXStack.push(midX)
        val (pyl, pyr) = py.partition { it.x < midX }
        state = ""
        divideAndConquer(px.subList(0, mid), pyl, mid)
        divideAndConquer(px.subList(mid, n), pyr, n - mid)


        val strip = py.filter { Math.abs(it.x - midX) < minD }
        highlightedPointsStack.push(strip)
        drawStrip = true
        state = "scanning strip"
        for (i in 0 until strip.size) {
            val p = strip[i]
            pointScanning = p
            for (j in i + 1 until strip.size) {
                if (strip[j].y - p.y >= minD)
                    break
                testPair(p, strip[j])
            }
            pointScanning = null
        }
        highlightedPointsStack.pop()

        state = ""
        drawStrip = false
        highlightedPointsStack.pop()
        midXStack.pop()
    }


    private fun clearCanvas() {
        ctx.save()
        ctx.fill = Color.WHITE
        ctx.fillRect(0.0, 0.0, canvas.width, canvas.height)
        ctx.restore()
    }


    private fun drawLine(line: Line?) {
        if (line == null)
            return
        ctx.save()
        ctx.setLineDashes(line.dash)
        ctx.stroke = line.color
        ctx.strokeLine(line.start.x, line.start.y, line.end.x, line.end.y)
        ctx.restore()
    }

    private fun drawPoint(p: Point?) {
        if (p == null)
            return
        ctx.save()
        ctx.fill = p.color
        ctx.fillOval(p.x - p.w / 2, p.y - p.h / 2, p.w, p.h)
        ctx.restore()
    }

    open inner class MyStack<E> : Stack<E>() {
        override fun peek(): E? {
            if (empty())
                return null
            return super.peek()
        }

        override fun push(item: E): E {
            super.push(item)
            repaint()
            return item
        }

        override fun pop(): E {
            val item = super.pop()
            repaint()
            return item
        }
    }
}

data class Point(
        val x: Double,
        val y: Double,
        var color: Color = Color.BLACK
) {

    var w = 10.0
    var h = 10.0
    fun distanceTo(other: Point): Double {
        return Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
    }
}

data class Line(
        val start: Point,
        val end: Point,
        var color: Color = Color.BLACK,
        var dash: Double = 0.0
) {
    constructor(x1: Double, y1: Double, x2: Double, y2: Double, color: Color = Color.BLACK, dash: Double = 0.0) : this(Point(x1, y1), Point(x2, y2), color, dash)

    private val length: Double = start.distanceTo(end)
    operator fun compareTo(other: Line): Int {
        val d = length - other.length
        if (d < 0) {
            return -1
        }
        if (d > 0) {
            return 1
        }
        return 0
    }
}

