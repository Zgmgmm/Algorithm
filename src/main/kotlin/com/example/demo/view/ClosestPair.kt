package com.example.demo.view

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import tornadofx.*
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CountDownLatch

class ClosestPair : View() {
    lateinit var canvas: Canvas
    lateinit var ctx: GraphicsContext

    private var MIN_X = 0
    private var MAX_X = 1300.0
    private var MIN_Y = 0
    private var MAX_Y = 800.0
    private var INTERVAL = 500L
    private var LINE_WIDTH=3.0
    val n = 20
    var points = arrayListOf<Point>()
    var shortestLine: Line? = null
    var testingLine: Line? = null
    var testingPoints = listOf<Point>()
    var bounds=arrayOf(-1.0,-1.0,-1.0)

    val cmpX=Comparator<Point> { p1, p2 ->
        val d=p1.x-p2.x
        if(d<0)
            return@Comparator -1
        if(d>0)
            return@Comparator 1
        0
    }
    val cmpY=Comparator<Point> { p1, p2 ->
        val d=p1.y-p2.y
        if(d<0)
            return@Comparator -1
        if(d>0)
            return@Comparator 1
        0
    }
    override val root = vbox {
        canvas = canvas {
            width = MAX_X
            height = MAX_Y
            setOnMouseClicked {
                latch.countDown()
            }
        }
        runAsync {
            ctx = canvas.graphicsContext2D
            for (i in 0 until n) {
                val x = Math.random() * (MAX_X - MIN_X) + MIN_X
                val y = Math.random() * (MAX_Y - MIN_Y) + MIN_Y
                points.add(Point(x, y))
            }
            points.forEach {
                drawPoint(it)
                println(it)
            }
            ctx.lineWidth=LINE_WIDTH
            Collections.sort(points, cmpX)
            closestPair(points, 0, points.size)
            testingPoints=listOf()
            testingLine=null
            repaint()
            println(shortestLine!!.length)
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
            var color: Color = Color.BLACK
    ) {
        constructor(x1: Double, y1: Double, x2: Double, y2: Double, color: Color = Color.BLACK) : this(Point(x1, y1), Point(x2, y2), color)

        val length: Double = start.distanceTo(end)
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

    fun clearCanvas() {
        ctx.save()
        ctx.fill = Color.WHITE
        ctx.fillRect(0.0, 0.0, canvas.width, canvas.height)
        ctx.restore()
    }

    fun repaint() {
        clearCanvas()
        ctx.save()
        println("${bounds[0]},${bounds[1]},${bounds[2]}")
        ctx.fill=Color.rgb(0,0,0,0.2)
        ctx.fillRect(bounds[0],0.0,bounds[2]-bounds[0],canvas.height)
        ctx.restore()
        drawLine(Line(bounds[1],0.0,bounds[1],canvas.height))
        points.forEach {
            drawPoint(it)
        }
        testingPoints.forEach {
            drawPoint(it)
        }
        drawLine(testingLine)
        drawLine(shortestLine)

    }


    private fun drawLine(line: Line?) {
        if (line == null)
            return
        ctx.save()
        ctx.stroke = line.color
        ctx.strokeLine(line.start.x, line.start.y, line.end.x, line.end.y)
        ctx.restore()
    }

    private fun drawPoint(p: Point) {
        ctx.save()
        ctx.fill = p.color
        ctx.fillOval(p.x-p.w/2, p.y-p.h/2, p.w, p.h)
        ctx.restore()
    }

    private fun testPair(p1: Point, p2: Point) {
//        println("testing pair ($p1,$p2)")
        sleep(INTERVAL)
        val line = Line(p1, p2, Color.YELLOW)
        testingLine = line
        repaint()
        println("p4")
        awaitClick()
        if (shortestLine == null || line < shortestLine!!) {
            line.color = Color.RED
            shortestLine = line
        }
        repaint()
        println("p5")
        awaitClick()
    }


    var latch: CountDownLatch = CountDownLatch(0)
    private fun awaitClick() {
        latch = CountDownLatch(1)
        latch.await()
    }



    private fun closestPair(points: List<Point>, start: Int, end: Int) {
        val len = end - start
        if (len < 2) {
            return
        }


        if (len == 2) {
            testPair(points[start], points[start + 1])
            return
        }
        testingPoints= listOf()
        for (i in start until end)
            testingPoints += points[i].copy(color = Color.YELLOW)

        repaint()
        println("p1")
        awaitClick()

        val mid = (start + end) / 2
        bounds[1]=points[mid].x
        repaint()
        println("p2")
        awaitClick()


        closestPair(points, start, mid)
        closestPair(points, mid, end)
        testingPoints= listOf()
        for (i in start until end)
            testingPoints += points[i].copy(color = Color.YELLOW)

        bounds[0]=points[mid].x - shortestLine!!.length
        bounds[1]=points[mid].x
        bounds[2]=points[mid].x + shortestLine!!.length
        repaint()
        println("p3")
        awaitClick()


        for (i in mid - 1 downTo 0) {
            if (points[mid].x - points[i].x > shortestLine?.length ?: Double.MAX_VALUE)
                break;
            for (j in mid until end) {
                if (points[j].x - points[i].x > shortestLine?.length ?: Double.MAX_VALUE)
                    break;
                testPair(points[i], points[j])
            }
        }

        bounds[0]=-1.0
        bounds[1]=-1.0
        bounds[2]=-1.0
        repaint()
        awaitClick()
    }

}
