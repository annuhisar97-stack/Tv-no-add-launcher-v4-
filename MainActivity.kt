package com.example.noadstvlauncher

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts

data class AppItem(val label:String,val pkg:String,val icon:android.graphics.drawable.Drawable)

class MainActivity : android.app.Activity() {
    private lateinit var root: LinearLayout
    private lateinit var appsBox: LinearLayout
    private lateinit var search: EditText
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var categoryBar: LinearLayout
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }
    private var allApps = mutableListOf<AppItem>()
    private var category = "All"
    private var cardDp = 130
    private var columns = 6

    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            prefs.edit().putString("wallpaper", uri.toString()).apply()
            applyWallpaper(uri)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        loadPrefs()
        buildUi()
        loadApps()
        tick()
    }

    private fun loadPrefs() {
        cardDp = prefs.getInt("cardDp",130)
        columns = prefs.getInt("columns",6)
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(28,20,28,20)
            setBackgroundColor(Color.rgb(7,7,10))
        }
        applySavedWallpaper()
        val top=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val title=TextView(this).apply { text="NoAds TV"; textSize=27f; setTextColor(Color.WHITE) }
        top.addView(title, LinearLayout.LayoutParams(0,70,1f))
        search=EditText(this).apply {
            hint="Search apps"; hintTextColor=0xff888888.toInt(); setTextColor(Color.WHITE)
            singleLine=true; setPadding(20,0,20,0); setBackgroundColor(0xff18181d.toInt())
        }
        top.addView(search, LinearLayout.LayoutParams(300,60))
        val gear=Button(this).apply { text="⚙"; textSize=22f; setOnClickListener{settings()} }
        top.addView(gear, LinearLayout.LayoutParams(75,65))
        root.addView(top)

        val info=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        clock=TextView(this).apply{textSize=28f;setTextColor(Color.WHITE)}
        date=TextView(this).apply{textSize=15f;setTextColor(0xffaaaaaa.toInt())}
        info.addView(clock,LinearLayout.LayoutParams(0,65,1f))
        info.addView(date,LinearLayout.LayoutParams(240,65))
        root.addView(info)

        categoryBar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        root.addView(categoryBar)
        appsBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(ScrollView(this).apply{addView(appsBox)},LinearLayout.LayoutParams(-1,0,1f))

        search.addTextChangedListener(object: android.text.TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){}
            override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){render()}
            override fun afterTextChanged(s:android.text.Editable?){}
        })
        setContentView(root)
    }

    private fun loadApps(){
        val pm=packageManager
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        var list=pm.queryIntentActivities(intent,0)
        if(list.isEmpty()) list=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        allApps=list.mapNotNull{
            val p=it.activityInfo.packageName
            try AppItem(it.loadLabel(pm).toString(),p,it.loadIcon(pm)) catch(e:Exception){null}
        }.distinctBy{it.pkg}.sortedBy{it.label.lowercase()}.toMutableList()
        render()
    }

    private fun render(){
        if(!::appsBox.isInitialized) return
        appsBox.removeAllViews()
        val q=search.text.toString().trim().lowercase()
        val favs=prefs.getStringSet("favs",emptySet()) ?: emptySet()
        val cats=prefs.getStringSet("cat_$category",emptySet()) ?: emptySet()
        val shown=allApps.filter{
            val okQ=q.isEmpty() || it.label.lowercase().contains(q)
            val okCat=category=="All" || (category=="Favorites" && favs.contains(it.pkg)) || (category!="Favorites" && cats.contains(it.pkg))
            okQ && okCat
        }.sortedWith(compareByDescending<AppItem>{favs.contains(it.pkg)}.thenBy{it.label.lowercase()})
        renderCategories()
        val rowH=cardDp+35
        var row:LinearLayout?=null
        shown.forEachIndexed{idx,a->
            if(idx%columns==0){ row=LinearLayout(this); appsBox.addView(row,LinearLayout.LayoutParams(-1,rowH)) }
            val card=Button(this).apply{
                text=(if(favs.contains(a.pkg))"★ " else "")+a.label
                textSize=15f; isAllCaps=false; setTextColor(Color.WHITE)
                setCompoundDrawablesWithIntrinsicBounds(null,a.icon,null,null)
                compoundDrawablePadding=10
                setPadding(8,8,8,8)
                setOnClickListener{startActivity(packageManager.getLaunchIntentForPackage(a.pkg))}
                setOnLongClickListener{appMenu(a);true}
                isFocusable=true; isFocusableInTouchMode=true
            }
            row!!.addView(card,LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(7,7,7,7)})
        }
    }

    private fun renderCategories(){
        categoryBar.removeAllViews()
        listOf("All","Favorites","Media","Games","Tools").forEach{c->
            val b=Button(this).apply{text=c;isAllCaps=false;setOnClickListener{category=c;render()}}
            categoryBar.addView(b,LinearLayout.LayoutParams(0,55,1f))
        }
    }

    private fun appMenu(a:AppItem){
        val favs=prefs.getStringSet("favs",emptySet())!!.toMutableSet()
        val opts= arrayOf(if(favs.contains(a.pkg))"Remove favorite" else "Add favorite","Media","Games","Tools")
        AlertDialog.Builder(this).setTitle(a.label).setItems(opts){_,which->
            when(which){
                0->{if(favs.contains(a.pkg))favs.remove(a.pkg)else favs.add(a.pkg);prefs.edit().putStringSet("favs",favs).apply()}
                1,2,3->{prefs.edit().putStringSet("cat_"+opts[which], (prefs.getStringSet("cat_"+opts[which],emptySet())?:emptySet())+a.pkg).apply()}
            }
            render()
        }.show()
    }

    private fun settings(){
        val items=arrayOf("🖼 Change wallpaper","🎨 Customize layout","📂 Manage categories","⭐ Manage favorites","🔄 Set as default launcher")
        AlertDialog.Builder(this).setTitle("Launcher settings").setItems(items){_,w->
            when(w){
                0->wallpaperPicker.launch("image/*")
                1->customize()
                2->categoryHelp()
                3->Toast.makeText(this,"Long-press any app to add/remove favorite.",Toast.LENGTH_LONG).show()
                4->try{startActivity(Intent(Settings.ACTION_HOME_SETTINGS))}catch(e:Exception){Toast.makeText(this,"Open Android Home app settings.",Toast.LENGTH_LONG).show()}
            }
        }.show()
    }

    private fun customize(){
        val labels=arrayOf("Dark theme","Light theme","Small cards","Medium cards","Large cards","5 columns","6 columns","7 columns","Show clock/date","Hide clock/date")
        AlertDialog.Builder(this).setTitle("Customize").setItems(labels){_,w->
            when(w){
                0->{root.setBackgroundColor(Color.rgb(7,7,10))}
                1->{root.setBackgroundColor(Color.rgb(235,235,240))}
                2->{cardDp=105;columns=7}
                3->{cardDp=130;columns=6}
                4->{cardDp=155;columns=5}
                5->{columns=5}
                6->{columns=6}
                7->{columns=7}
                8->{clock.visibility=View.VISIBLE;date.visibility=View.VISIBLE}
                9->{clock.visibility=View.GONE;date.visibility=View.GONE}
            }
            prefs.edit().putInt("cardDp",cardDp).putInt("columns",columns).apply()
            render()
        }.show()
    }

    private fun categoryHelp(){
        AlertDialog.Builder(this).setTitle("App categories")
            .setMessage("Long-press an app → choose Media, Games or Tools. Then open that category from the top bar.")
            .setPositiveButton("OK",null).show()
    }

    private fun applySavedWallpaper(){
        prefs.getString("wallpaper",null)?.let{try{applyWallpaper(Uri.parse(it))}catch(_:Exception){}}
    }
    private fun applyWallpaper(uri:Uri){
        try{
            val d=android.graphics.drawable.Drawable.createFromStream(contentResolver.openInputStream(uri),"wallpaper")
            root.background=d
        }catch(_:Exception){}
    }

    private fun tick(){
        val h=android.os.Handler(mainLooper)
        val r=object:Runnable{
            override fun run(){
                if(::clock.isInitialized){
                    val now=java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(java.util.Date())
                    val ds=java.text.SimpleDateFormat("EEE, dd MMM yyyy",java.util.Locale.getDefault()).format(java.util.Date())
                    clock.text=now;date.text=ds
                }
                h.postDelayed(this,30000)
            }
        }
        h.post(r)
    }
}
