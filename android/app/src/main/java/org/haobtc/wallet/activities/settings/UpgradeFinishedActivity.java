package org.haobtc.wallet.activities.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.haobtc.wallet.R;
import org.haobtc.wallet.activities.base.BaseActivity;
import org.haobtc.wallet.aop.SingleClick;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.com.heaton.blelibrary.ble.Ble;

public class UpgradeFinishedActivity extends BaseActivity {
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.tet_version)
    TextView tetVersion;
    @BindView(R.id.btn_finish)
    Button btnFinish;

    @Override
    public int getLayoutId() {
        return R.layout.activity_upgrade_finished;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
    }

    @Override
    public void initData() {

    }

    @SingleClick
    @OnClick({R.id.img_back, R.id.btn_finish})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
            case R.id.btn_finish:
                Ble.getInstance().disconnectAll();
                finish();
                break;
        }
    }
}