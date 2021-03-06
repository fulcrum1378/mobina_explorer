package ir.mahdiparastesh.mobinaexplorer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.mlkit.common.MlKit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import ir.mahdiparastesh.mobinaexplorer.misc.TfUtils
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

class Analyzer(val c: Context, val isTest: Boolean = false) {
    var model: ByteBuffer
    private val detector by lazy {
        try {
            MlKit.initialize(c)
        } catch (ignored: IllegalStateException) {
        }
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    init {
        c.resources.assets.openFd(MODEL_FILE).apply {
            model = FileInputStream(fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            close()
        }
    }

    inner class Subject(bmp: Bitmap?, private val listener: OnFinished) {
        private var results: Results? = null

        constructor(bar: ByteArray, listener: OnFinished) : this(barToBmp(bar), listener)

        init {
            if (bmp == null) Transit(listener, null)
            else detector.process(InputImage.fromBitmap(bmp, 0)).addOnSuccessListener { wryFaces ->
                results = Results(wryFaces.size, listener, wryFaces)
                var ff = 0
                for (f in wryFaces) TfUtils.rotate(bmp, f.headEulerAngleZ).apply {
                    detector.process(InputImage.fromBitmap(this, 0)).addOnSuccessListener { faces ->
                        if (faces.isNullOrEmpty() || ff >= faces.size)
                            results!!.result(null)
                        else {
                            val cropped = if (faces[ff].boundingBox != f.boundingBox)
                                crop(this, faces[ff])
                            else this
                            if (isTest && results!!.cropped == null)
                                results!!.cropped = cropped
                            results!!.result(Result(compare(cropped)))
                            ff++
                        }
                    }.addOnFailureListener { results!!.result(null) }
                }
            }.addOnFailureListener { Transit(listener, null) }
        }

        private fun crop(raw: Bitmap, f: Face): Bitmap {
            val left = if (f.boundingBox.left < 0) 0 else f.boundingBox.left
            val top = if (f.boundingBox.top < 0) 0 else f.boundingBox.top
            val right = if (f.boundingBox.right > raw.width) raw.width else f.boundingBox.right
            val bottom = if (f.boundingBox.bottom > raw.height) raw.height else f.boundingBox.bottom
            return Bitmap.createBitmap(raw, left, top, right - left, bottom - top)
        }

        private fun compare(cropped: Bitmap): FloatArray {
            val input = arrayOf(TfUtils.tensor(cropped))
            val output = Array(input.size) { FloatArray(MODEL_PEOPLE.size) }
            Interpreter(model).use {
                it.run(input, output)
                it.close()
            }
            return output[0]
        }

        @Suppress("unused")
        fun show(cl: ConstraintLayout, w: Int, h: Int) {
            if (results == null) return
            cl.removeAllViews()
            for (obj in results!!.wryFaces) cl.addView(View(cl.context).apply {
                val bound = obj.boundingBox
                layoutParams = ConstraintLayout.LayoutParams(
                    relative(abs(bound.left - bound.right), w, cl.width).toInt(),
                    relative(abs(bound.top - bound.bottom), h, cl.height).toInt()
                ).apply {
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                }
                this.id = id
                translationX = relative(bound.left, w, cl.width)
                translationY = relative(bound.top, h, cl.height)
                background = ContextCompat.getDrawable(cl.context, R.drawable.detected)
                setOnClickListener { }
            })
        }

        private fun relative(num: Int, older: Int, newer: Int) =
            (newer.toFloat() / older.toFloat()) * num.toFloat()
    }

    fun interface OnFinished {
        fun onFinished(results: Results?)
    }

    inner class Results(
        private val expect: Int, private val listener: OnFinished, var wryFaces: List<Face>
    ) : ArrayList<Result>() {
        private var reality = 0
        var cropped: Bitmap? = null

        init {
            if (expect == 0) Transit(listener, this)
        }

        fun result(e: Result?) {
            reality++
            if (e != null) add(e)
            if (reality == expect) Transit(listener, this)
        }

        fun anyQualified() = any { it.qualified }

        fun mobina() = filter { it.qualified }.maxOf { it.prob[0] }
    }

    class Result(
        val prob: FloatArray, var qualified: Boolean = prob[0] > CANDIDATURE
    ) {
        // fun alike() = prob.indexOfFirst { it == max(prob.toList()) }
    }

    inner class Transit(val listener: OnFinished, val results: Results?) {
        init {
            /*if (!results.isNullOrEmpty()) Log.println(Log.DEBUG, "ANALYZER",
                "${Crawler.un} =>${Gson().toJson(Models.PLURAL.labels[results[0].like])}: " +
                    "${results[0].prob[results[0].like]}")*/
            if (!isTest) Crawler.handler?.obtainMessage(Crawler.HANDLE_ML_KIT, this)
                ?.sendToTarget()
            else Panel.handler?.obtainMessage(Panel.Action.HANDLE_TEST.ordinal, this)
                ?.sendToTarget()
        }
    }

    companion object {
        const val MODEL_FILE = "fifty_epochs.tflite"
        const val MODEL_SIZE = 224
        const val CANDIDATURE = 0.1f

        // A model trained in 50 epochs worked ~10 times better than a model trained in 5 epochs!
        @Suppress("SpellCheckingInspection")
        val MODEL_PEOPLE = arrayOf(
            "Mobina", "aimi", "amin", "amir", "amirali", "ava_bizjak", "braydan",
            "celina_krogmann", "dad", "david_laid", "diana_deets", "dominik", "elisany",
            "elizabeth_debicki", "emily_feld", "hannah_min", "hannah_owo", "hasani", "hssp",
            "jun_xi", "kiernan", "lay_heegaard", "mahdi", "mark", "maryam", "mina_vahid",
            "mohaddeseh", "nana_pi", "natasha", "olivier", "pham", "queeny", "rike", "sara_kim",
            "sarah_rae_mayne", "sophia_lariz", "sukaru", "vivian", "william", "yekoong"
        )

        fun barToBmp(bar: ByteArray?): Bitmap? =
            if (bar != null) BitmapFactory.decodeByteArray(bar, 0, bar.size) else null
    }
}
