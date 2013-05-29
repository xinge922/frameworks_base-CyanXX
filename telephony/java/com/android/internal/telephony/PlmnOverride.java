package com.android.internal.telephony;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

public class PlmnOverride {
    private HashMap<String, String> CarrierPlmnMap;


    static final String LOG_TAG = "GSM";
    static final String PARTNER_SPN_OVERRIDE_PATH ="etc/mobile-conf.xml";

    public PlmnOverride () {
        CarrierPlmnMap = new HashMap<String, String>();
        loadPlmnOverrides();
    }

    public boolean containsCarrier(String carrier) {
        return CarrierPlmnMap.containsKey(carrier);
    }

    public String getPlmn(String carrier) {
        return CarrierPlmnMap.get(carrier);
    }

    private void loadPlmnOverrides() {
        FileReader plmnReader;

        final File plmnFile = new File(Environment.getRootDirectory(),
                PARTNER_SPN_OVERRIDE_PATH);

        try {
            plmnReader = new FileReader(plmnFile);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_SPN_OVERRIDE_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(plmnReader);

            XmlUtils.beginDocument(parser, "Mobiles");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();

                if (!"Mobile".equals(name)) {
                    break;
                }

                String numeric = parser.getAttributeValue(null, "Numeric");
                String data    = parser.getAttributeValue(null, "MobileName");


                CarrierPlmnMap.put(numeric, data);
            }
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Exception in plmn-conf parser " + e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception in plmn-conf parser " + e);
        }
    }

}
