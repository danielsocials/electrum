package org.haobtc.wallet.activities.set;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chaquo.python.PyObject;

import org.haobtc.wallet.R;
import org.haobtc.wallet.activities.base.BaseActivity;
import org.haobtc.wallet.adapter.QuetationChooseAdapter;
import org.haobtc.wallet.bean.CNYBean;
import org.haobtc.wallet.utils.Daemon;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QuetationChooseActivity extends BaseActivity {

    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.recl_Quetation)
    RecyclerView reclQuetation;
    private ArrayList<CNYBean> exchangeList;
    private SharedPreferences.Editor edit;
    private int exChange;

    @Override
    public int getLayoutId() {
        return R.layout.activity_quetation_choose;
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
        exchangeList = new ArrayList<>();
        //get exchanges list
        getExchangelist();

    }

    private void getExchangelist() {
        PyObject get_exchanges = null;
        try {
            get_exchanges = Daemon.commands.callAttr("get_exchanges");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (get_exchanges!=null){

            List<PyObject> pyObjects = get_exchanges.asList();
            for (int i = 0; i < pyObjects.size(); i++) {
                CNYBean cnyBean = new CNYBean(pyObjects.get(i).toString(), false);
                exchangeList.add(cnyBean);
            }
        }
        QuetationChooseAdapter quetationChooseAdapter = new QuetationChooseAdapter(QuetationChooseActivity.this,exchangeList,exChange);
        reclQuetation.setAdapter(quetationChooseAdapter);
        quetationChooseAdapter.setOnLisennorClick(new QuetationChooseAdapter.onLisennorClick() {
            @Override
            public void ItemClick(int pos) {
                try {
                    Daemon.commands.callAttr("set_exchange",exchangeList.get(pos));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                edit.putInt("exChange",pos);
                edit.apply();
            }
        });

    }

    @OnClick({R.id.img_back})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
        }
    }
}