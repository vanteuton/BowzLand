package android.bowz.fr.bowzland

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_settings.*


class MainSettingsActivity : AppCompatActivity() {


    //TODO : Je suis pas bien sur que j'appelle mon service de job schedulation :'/
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_settings)

        val mJobScheduler = (getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler)
        val builder = JobInfo.Builder(1, ComponentName(packageName, MyService::class.java.name))
        builder.setPeriodic( 3000 )
        buttonScheduleJob.setOnClickListener {
            when(mJobScheduler.schedule(builder.build())){
                JobScheduler.RESULT_FAILURE -> {
                    buttonScheduleJob.setBackgroundColor(Color.RED)
                    buttonScheduleJob.text = "Erreur"
                    buttonScheduleJob.isEnabled = false
                }
                JobScheduler.RESULT_SUCCESS -> {
                    buttonScheduleJob.text = "annuler les jobs"
                    buttonScheduleJob.setOnClickListener {
                        mJobScheduler.cancelAll()
                        buttonScheduleJob.text = "relancer le job"
                    }
                }
            }
        }
    }

}
