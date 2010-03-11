package eu.gaetangrigis.cts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.R.integer;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class Main extends Activity {
    /** Called when the activity is first created. */
	private Button check;
	private Button gcode;
	private Button clear;
	private TextView t;
	private EditText station;
	private EditText nstation;
	private EditText heure;
	private EditText minute;
	private TableLayout table;
	
	private int mHeure;
	private int mMinute;
	private TimePickerDialog.OnTimeSetListener mTimeSetListener;
	
	final Calendar c = Calendar.getInstance();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

//Recuperation des champs
        table = (TableLayout)findViewById(R.id.maTable);
        station = (EditText)findViewById(R.id.idStation);
        heure = (EditText)findViewById(R.id.heure);
        minute = (EditText)findViewById(R.id.minute);
        nstation = (EditText)findViewById(R.id.nomStation);
    	check=(Button)findViewById(R.id.check);
    	clear=(Button)findViewById(R.id.clear);
    	gcode=(Button)findViewById(R.id.getNom);
        t =(TextView) findViewById(R.id.information);

//Texte par défaut
        mHeure = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        t.setText("Entrez les informations demandées");
        station.setText("75");
        mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
	        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	            mHeure = hourOfDay;
	            mMinute = minute;
	            updateTime();
	        }
	    };

        updateTime();
        gcode.setOnClickListener(new OnClickListener() {public void onClick(View v) {clearResearch();getStationCode(nstation.getText().toString());}});
        check.setOnClickListener(new OnClickListener() {public void onClick(View v) {clearResearch();startResearch();}});
		clear.setOnClickListener(new OnClickListener() {public void onClick(View v) {clearResearch();}});
		heure.setOnFocusChangeListener(new OnFocusChangeListener() {public void onFocusChange(View v, boolean hasFocus) {if(hasFocus)showDialog(0);}});
		minute.setOnFocusChangeListener(new OnFocusChangeListener() {public void onFocusChange(View v, boolean hasFocus) {if(hasFocus)showDialog(0);}});
    }
    
    public void clearResearch(){table.removeAllViews();}
    
    public void startResearch()
    {
    	t.setText("Recherche en cours");
        HttpGet site   = new HttpGet("http://tr.cts-strasbourg.fr/HorTRweb/ResultatsHoraires.aspx?arret="+station.getText()+"&type=TOUS&heure="+heure.getText()+"&min="+minute.getText());
        HttpClient cli = new DefaultHttpClient();
        HttpResponse resp;
        String res;
        String content=new String();
        String ret = new String();
        boolean dTab=false;
        try {
        	resp = cli.execute(site);
        	BufferedReader read = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
        	while((res=read.readLine())!=null)
        	{
        		if(res.matches(".*<table class=.depart.*")){dTab=true;}
        		if(dTab==true&&res.matches(".*</table>.*")){dTab=false;break;}
        		if(dTab==true){content+=res;}
        	}
        	Pattern p = Pattern.compile("<td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td>");
        	Matcher m = p.matcher(content);
        	while(m.find())
        	{
        		for(int i=1;i<=m.groupCount();i++){ret+=m.group(i)+" ";}
        		table.addView(addTextRow(ret));
        		ret="";
        	}
        }
        catch (ClientProtocolException e) {t.setText(e.getMessage());}
		catch (IOException e) {t.setText(e.getMessage());}
		finally{t.setText("Recherche terminé");}
    }
    
    public TableRow addTextRow(String text)
    {
    	TableRow tr = new TableRow(this);
        TextView tt = new TextView(this);
        tt.setText(text);
        tr.addView(tt);
		return tr;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case 0:
			return new TimePickerDialog(this,
                    mTimeSetListener, mHeure, mMinute, false);
        }
        return null;
    }
    
    public void updateTime()
    {
    	String add="";
        heure.setText(mHeure+"");
        if(mMinute<10)
        	add="0";
        minute.setText(add+mMinute);
    }
    
    public String getStationCode(String nom)
    {
    	HttpGet site   = new HttpGet("http://tr.cts-strasbourg.fr/HorTRweb/RechercheCodesArrets.aspx");
        HttpClient cli = new DefaultHttpClient();
        HttpResponse resp;
        String res;
        String ret = new String();
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        String name="";
        String content=new String();
        int nbCode = 0;
        boolean dTab=false;
        try {
        	resp = cli.execute(site);
        	BufferedReader read = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
        	while((res=read.readLine())!=null)
        	{
            	Pattern p = Pattern.compile("<input(.*)/>");
            	Pattern param = Pattern.compile("(name|value)=\"([^\"]*)\"");
            	Matcher m = p.matcher(res);
            	while(m.find())
            	{
            		for(int i=1;i<=m.groupCount();i++){ret+=m.group(i);}
            		Matcher mparam = param.matcher(ret);
            		ret="";
            		while(mparam.find())
            		{
            			if(mparam.groupCount()>=2)
            			{
            				if(mparam.group(1).trim().matches("name"))
            				{
            					name=mparam.group(2);
            					if(mparam.group(2).endsWith("nouvellerecherche")){name="";}
            					if(mparam.group(2).endsWith("LibArret"))
            					{
            						pairs.add(new BasicNameValuePair(name, nom));
            						name="";ret=pairs.toString();
            					}
            				}
            				if(mparam.group(1).trim().matches("value"))
            				{
            					ret=name;
            					if(name!="")
            						pairs.add(new BasicNameValuePair(name, mparam.group(2)));
            					name="";
            				}
            			}
            		}
            	}
        	}
        	pairs.add(new BasicNameValuePair("_EVENTTARGET", ""));
        	pairs.add(new BasicNameValuePair("_EVENTARGUMENT", ""));
        	HttpPost post=new HttpPost("http://tr.cts-strasbourg.fr/HorTRweb/RechercheCodesArrets.aspx");
    		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
    		post.setEntity(new UrlEncodedFormEntity(pairs));
    		resp = cli.execute(post);
        	read = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
        	while((res=read.readLine())!=null)
        	{
        		if(res.matches(".*<table class=.depart.*")){dTab=true;}
        		if(dTab==true&&res.matches(".*</table>.*")){dTab=false;break;}
        		if(dTab==true){content+=res+"\n";}
        	}
        	Pattern p = Pattern.compile("<a[^>]*>([^<]*)</a>");
        	Matcher m = p.matcher(content);
        	while(m.find())
        	{
        		if(m.groupCount()>0)
        		{
	        		ret=m.group(1);
        			station.setText(m.group(1));//put the last
	        		table.addView(addTextRow(ret));
	        		ret="";
        		}
        		nbCode++;
        	}
        	if((nbCode/2)==1){clearResearch();startResearch();}
        }
        catch (ClientProtocolException e) {t.setText(e.getMessage());}
		catch (IOException e) {t.setText(e.getMessage());}
		return nom;
    }
}