
package com.example.learnflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.learnflow.data.model.Request
import com.example.learnflow.data.model.Session
import com.example.learnflow.data.remote.RetrofitClient
import com.github.mikephil.charting.data.Entry
import com.example.learnflow.data.repository.LoginRepository
import com.example.learnflow.data.repository.SessionRepository
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var userId:String;
    private lateinit var loginRepository:LoginRepository;
    private lateinit var loaderContainer: FrameLayout
    private lateinit var btn:FrameLayout;
    private lateinit var greeting:TextView;
    private lateinit var txtAccuracy:TextView;
    private lateinit var txtConsistancy:TextView;
    private lateinit var btnTakeTest: MaterialButton;
    private lateinit var txtStreak:TextView;
    private lateinit var txtCount :TextView;
    private lateinit var sessionRepository : SessionRepository;
    private lateinit var sessionList : MutableList<Session>;
    private lateinit var performanceChart: LineChart

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Notification permission denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        loaderContainer = findViewById(R.id.loaderContainer)
        btn = findViewById(R.id.navAI)
        greeting = findViewById(R.id.txtGreeting);
        txtCount = findViewById(R.id.txtCount);
        txtStreak = findViewById(R.id.txtStreak);
        txtAccuracy = findViewById(R.id.txtAccuracy);
        txtConsistancy = findViewById(R.id.txtConsistancy);
        var focus = findViewById<LinearLayout>(R.id.navFocus)
        var session = findViewById<LinearLayout>(R.id.navSession);
        performanceChart = findViewById(R.id.performanceChart)
        btnTakeTest = findViewById(R.id.btnTakeTest);

        sessionRepository = SessionRepository(RetrofitClient.api);
        loginRepository = LoginRepository(RetrofitClient.api)
        btn.visibility = View.GONE
        showLoader()
        initializeGraph()
        userLoginCheck();

        btn.setOnClickListener {
            var intent = Intent(this@MainActivity, ChatTopic::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        focus.setOnClickListener {
            var intent = Intent(this@MainActivity, AppBlockerActivity::class.java)
            startActivity(intent)
        }

        btnTakeTest.setOnClickListener{
            if(userId.isEmpty()){
                Toast.makeText(this@MainActivity,"please wait we are setting up",Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }
            var intent = Intent(this@MainActivity,TestCreator::class.java);
            startActivity(intent);
        }

        session.setOnClickListener{
            if(userId.isEmpty()){
                Toast.makeText(this@MainActivity,"please wait we are setting up",Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }
            var intent = Intent(this@MainActivity,SessionCreate::class.java);
            intent.putExtra("userId",userId);
            startActivity(intent);
        }
    }

    private fun userLoginCheck() {
        lifecycleScope.launch {
            try {
                val response = loginRepository.userIsLoggedIn()
                if (!response.success) {
                    hideLoader();
                    val intent = Intent(this@MainActivity, Promo::class.java)
                    startActivity(intent)
                    finish()
                    return@launch
                }
                userId = response.userId
                if (userId.isEmpty()) {
                    hideLoader();
                    Toast.makeText(this@MainActivity, "Setting up the app, please wait", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                btn.visibility = View.VISIBLE
                greeting.text = "Hello, ${response.name}! " + greeting.text.toString()
                userAnalysis()
            } catch (e: Exception) {
                hideLoader();
                val intent = Intent(this@MainActivity, Promo::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun userAnalysis(){
        lifecycleScope.launch{
            try{
                var response = sessionRepository.userAnalysis(Request(userId));
                if(response.success){
                    sessionList();
                    txtCount.text = response.analysis.sessionCompletion.toString()+"/"+response.analysis.sessionCount;
                    txtStreak.text = response.analysis.streak.toString();
                    txtAccuracy.text = response.analysis.accuracy.toString();
                    txtConsistancy.text = response.analysis.consistancy.toString()+"%";
                }
            }catch(e:Exception){
                println(e.message);
            }finally{
                hideLoader();
            }
        }
    }

    private suspend fun sessionList(){
        lifecycleScope.launch{
            try{
                var response = sessionRepository.sessionList(Request(userId));
                if(response.success){
                    sessionList = response.sessionList;
                    updatePerformanceGraph(sessionList);
                }
            }catch(e:Exception){
                Toast.makeText(this@MainActivity,"Data recovery has failed",Toast.LENGTH_SHORT).show();
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (::userId.isInitialized && userId.isNotEmpty()) {
            userAnalysis()
        }
    }

    private fun showLoader() {
        loaderContainer.visibility = View.VISIBLE
        loaderContainer.bringToFront()
    }

    private fun hideLoader() {
        loaderContainer.visibility = View.GONE
    }


    private fun initializeGraph() {
        performanceChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                textColor = Color.LTGRAY
                textSize = 10f
                setDrawGridLines(true)
                gridColor = Color.DKGRAY
            }
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.LTGRAY
                textSize = 10f
                setDrawGridLines(false)
                granularity = 1f
            }
            setNoDataText("Complete your first study session!")
            setNoDataTextColor(Color.LTGRAY)
        }
    }

    private fun updatePerformanceGraph(sessionList: MutableList<Session>) {
        val createdEntries = ArrayList<Entry>()
        val completedEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = calendar.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)
            val start = dayStart.timeInMillis
            val end = start + 24 * 60 * 60 * 1000L
            val daySessions = sessionList.filter {
                it.startTime >= start && it.startTime < end
            }
            val created = daySessions.size
            val completed = daySessions.count {
                it.status.equals("completed", ignoreCase = true)
            }
            createdEntries.add(Entry((6 - i).toFloat(), created.toFloat()))
            completedEntries.add(Entry((6 - i).toFloat(), completed.toFloat()))

            labels.add(
                SimpleDateFormat("EEE", Locale.getDefault())
                    .format(dayStart.time)
            )
        }
        val createdDataSet = LineDataSet(createdEntries, "Created").apply {
            color = Color.rgb(255, 152, 0)
            setCircleColor(Color.rgb(255, 152, 0))
            lineWidth = 2.5f
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }
        val completedDataSet = LineDataSet(completedEntries, "Completed").apply {
            color = Color.rgb(76, 175, 80)
            setCircleColor(Color.rgb(76, 175, 80))
            lineWidth = 2.5f
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }
        val highestSessionCount = maxOf(
            createdEntries.maxOfOrNull { it.y } ?: 0f,
            completedEntries.maxOfOrNull { it.y } ?: 0f
        )
        performanceChart.apply {
            data = LineData(createdDataSet, completedDataSet)
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = highestSessionCount + 5f
            axisLeft.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 6f
            animateX(1000)
            invalidate()
        }
    }
}
