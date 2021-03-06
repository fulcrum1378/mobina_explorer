package ir.mahdiparastesh.mobinaexplorer.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ir.mahdiparastesh.mobinaexplorer.R

object UiTools {
    private const val PROFILE = "https://www.instagram.com/%s/"

    fun color(c: Context, res: Int) = ContextCompat.getColor(c, res)

    @Suppress("DEPRECATION")
    fun shake(c: Context, dur: Long = 55L) {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            .vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun vis(v: View, bb: Boolean = true) {
        v.visibility = if (bb) View.VISIBLE else View.GONE
    }

    fun bytes(c: Context, l: Long): String {
        val units = c.resources.getStringArray(R.array.bytes)
        var gig = 0L
        var meg = 0L
        var kil = 0L
        var ll = l
        if (ll >= 1073741824L) {
            gig = ll / 1073741824L
            ll %= 1073741824L
        }
        if (ll >= 1048576L) {
            meg = ll / 1048576L
            ll %= 1048576L
        }
        if (ll >= 1024L) {
            kil = ll / 1024L
            ll %= 1024L
        }
        return arrayListOf<String>().apply {
            if (gig > 0) add("$gig ${units[3]}")
            if (meg > 0) add("$meg ${units[2]}")
            if (kil > 0 && gig == 0L) add("$kil ${units[1]}")
            if (meg == 0L) add("$ll ${units[0]}")
        }.joinToString(", ")
    }

    fun square(v: View) {
        val dm = v.context.resources.displayMetrics
        v.layoutParams = (v.layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentHeight =
                (dm.widthPixels.toFloat() / dm.heightPixels) * matchConstraintPercentWidth
        }
    }

    fun wave(c: Context, v: View, res: Int) {
        if (v.parent !is ConstraintLayout) return
        val parent = v.parent as ConstraintLayout
        val ex = View(c).apply {
            background = ContextCompat.getDrawable(c, res)
            translationY = v.translationY * .328f
            scaleX = v.scaleX
            scaleY = v.scaleY
            alpha = v.alpha
        }
        val dim = (v.width * .24f).toInt()
        parent.addView(ex, parent.indexOfChild(v) + 1,
            ConstraintLayout.LayoutParams(dim, dim).apply {
                topToTop = v.id; leftToLeft = v.id; rightToRight = v.id; bottomToBottom = v.id
                verticalBias = .191f
            })

        AnimatorSet().setDuration(1003L).apply {
            playTogether(
                ObjectAnimator.ofFloat(ex, "scaleX", ex.scaleX * 2f),
                ObjectAnimator.ofFloat(ex, "scaleY", ex.scaleY * 2f),
                ObjectAnimator.ofFloat(ex, "alpha", 0f)
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    parent.removeView(ex)
                }
            })
            start()
        }
    }

    fun openProfile(c: ComponentActivity, user: String) {
        c.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(PROFILE.format(user)))
            //.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
