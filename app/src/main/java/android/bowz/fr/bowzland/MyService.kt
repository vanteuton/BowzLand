package android.bowz.fr.bowzland

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Handler
import android.os.Message
import android.widget.Toast


class MyService : JobService() {
    /**
     * onStopJob est appellée pour arrêter un job qui prendrais trop de temps (donc qui aurait retourné TRUE sur onStartJob)
     */
    override fun onStopJob(params: JobParameters?): Boolean {
        Toast.makeText(applicationContext, "STOP", Toast.LENGTH_SHORT).show()
        mJobHandler.removeMessages( 1 )
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Toast.makeText(applicationContext, "OnStartBro", Toast.LENGTH_SHORT).show()
        //onStartJob retourne false qd le job s'est exécuté rapidement, true qd le job a besoin de plus de temps.
        mJobHandler.sendMessage( Message.obtain( mJobHandler, 1, params ) )
        return true
    }

    private val mJobHandler = Handler(Handler.Callback { msg ->
        Toast.makeText(applicationContext, "Handler de m****", Toast.LENGTH_SHORT).show()
        jobFinished(msg.obj as JobParameters, false)
        true
    })
}