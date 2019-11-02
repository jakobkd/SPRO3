package com.teamez.ezrobotac;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.List;
import java.util.Objects;


public class CustomDialogClass extends AppCompatDialogFragment {

    private AppCompatEditText ssid, password;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View view = inflater.inflate(R.layout.add_dialog, null);
        ssid = view.findViewById(R.id.ssid_input);
        password = view.findViewById(R.id.password_input);

        builder.setView(view)
                .setTitle("Add Network")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String mSSID = Objects.requireNonNull(ssid.getText()).toString();
                        String mPass = Objects.requireNonNull(password.getText()).toString();
                        setWifiConfig(mSSID, mPass);

                    }
                });



        return builder.create();
    }


    public void setWifiConfig(String ssid, String sharedKey) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";   // Please note the quotes. String should contain ssid in quotes

        conf.preSharedKey = "\"" + sharedKey + "\"";

        conf.hiddenSSID = false;
        conf.status = WifiConfiguration.Status.ENABLED;
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

        WifiManager wifiManager = (WifiManager) Objects.requireNonNull(getActivity()).getSystemService(Context.WIFI_SERVICE);

        assert wifiManager != null;
        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {

                wifiManager.disconnect();

                wifiManager.enableNetwork(i.networkId, true);

                wifiManager.reconnect();

                wifiManager.saveConfiguration();
                break;
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Activity activity = getActivity();
        if (activity instanceof NetworkDialogCloseListener) {
            ((NetworkDialogCloseListener)activity).handleDialogClose(dialog);
        }
    }
}
