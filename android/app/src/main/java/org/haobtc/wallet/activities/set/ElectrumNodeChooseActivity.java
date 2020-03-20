package org.haobtc.wallet.activities.set;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONObject;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chaquo.python.PyObject;

import org.haobtc.wallet.R;
import org.haobtc.wallet.activities.base.BaseActivity;
import org.haobtc.wallet.adapter.ElectrumListAdapter;
import org.haobtc.wallet.bean.CNYBean;
import org.haobtc.wallet.event.SendMoreAddressEvent;
import org.haobtc.wallet.utils.Daemon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ElectrumNodeChooseActivity extends BaseActivity {

    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.recl_nodeChose)
    RecyclerView reclNodeChose;
    @BindView(R.id.btn_Finish)
    Button btnFinish;
    private PyObject get_server_list;
    private ArrayList<CNYBean> electrumList;
    private int exChange;
    private SharedPreferences.Editor edit;
    private ArrayList<SendMoreAddressEvent> addressEvents;

    @Override
    public int getLayoutId() {
        return R.layout.activity_electrum_node_choose;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
        SharedPreferences preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        edit = preferences.edit();
        exChange = preferences.getInt("exChange", 0);

    }

    @Override
    public void initData() {
        electrumList = new ArrayList<>();
        addressEvents = new ArrayList<>();
        //get electrum list
        getElectrumData();

    }

    private void getElectrumData() {
        try {
            get_server_list = Daemon.commands.callAttr("get_server_list");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (get_server_list != null) {
            SendMoreAddressEvent sendMoreAddressEvent = new SendMoreAddressEvent();
            String get_server = get_server_list.toString();
            Log.i("get_server_list", "get_server_list: "+get_server);
            Map<String, Object> jsonToMap = JSONObject.parseObject(get_server);
            Set<String> keySets = jsonToMap.keySet();
            Iterator<String> ki = keySets.iterator();
            while (ki.hasNext()) {
                String k = ki.next();
                //get key
                String v = jsonToMap.get(k).toString();
                Map<String, Object> vToMap = JSONObject.parseObject(v);
                Set<String> vkeySets = vToMap.keySet();
                Iterator<String> vki = vkeySets.iterator();
                while (vki.hasNext()) {
                    String vk = vki.next();
                    if ("s".equals(vk)) {
                        String vvalue = vToMap.get(vk).toString();
                        String strElectrum = k + ":" + vvalue;
                        sendMoreAddressEvent.setInputAddress(k);
                        sendMoreAddressEvent.setInputAmount(vvalue);
                        addressEvents.add(sendMoreAddressEvent);
                        CNYBean cnyBean = new CNYBean(strElectrum, false);
                        electrumList.add(cnyBean);
                    }
                }
            }
            ElectrumListAdapter electrumListAdapter = new ElectrumListAdapter(ElectrumNodeChooseActivity.this, electrumList, exChange);
            reclNodeChose.setAdapter(electrumListAdapter);
            electrumListAdapter.setOnLisennorClick(new ElectrumListAdapter.onLisennorClick() {
                @Override
                public void ItemClick(int pos) {
                    try {
                        Daemon.commands.callAttr("set_server", addressEvents.get(pos).getInputAddress(), addressEvents.get(pos).getInputAmount());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    edit.putInt("exChange", pos);
                    edit.apply();
                }
            });
        }
    }


    @OnClick({R.id.img_back, R.id.btn_Finish})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.btn_Finish:
                View view1 = LayoutInflater.from(this).inflate(R.layout.add_node_layout, null, false);
                AlertDialog alertDialog = new AlertDialog.Builder(this).setView(view1).create();
                ImageView img_Cancle = view1.findViewById(R.id.cancel_select_wallet);

                view1.findViewById(R.id.btn_enter_wallet).setOnClickListener(v -> {
                    EditText edit_ip = view1.findViewById(R.id.edit_ip);
                    EditText edit_port = view1.findViewById(R.id.edit_port);
                    if (TextUtils.isEmpty(edit_ip.getText().toString())) {
                        mToast(getString(R.string.please_inputIp));
                        return;
                    }
                    if (TextUtils.isEmpty(edit_port.getText().toString())) {
                        mToast(getString(R.string.please_inputPort));
                        return;
                    }

                    try {
                        Daemon.commands.callAttr("set_server", edit_ip.getText().toString(), edit_port.getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (e.getMessage().contains("ValueError")){
                            mToast(getString(R.string.ipOrportWrong));
                        }
                        return;
                    }
                    electrumList.clear();
                    getElectrumData();
                    mToast(getString(R.string.add_finished));
                    alertDialog.dismiss();

                });
                img_Cancle.setOnClickListener(v -> {
                    alertDialog.dismiss();
                });
                alertDialog.show();
                break;
        }
    }

}