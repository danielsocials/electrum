package org.haobtc.onekey.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.transition.TransitionManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.base.Strings;

import org.greenrobot.eventbus.EventBus;
import org.haobtc.onekey.R;
import org.haobtc.onekey.bean.PyResponse;
import org.haobtc.onekey.constant.Constant;
import org.haobtc.onekey.constant.StringConstant;
import org.haobtc.onekey.event.GotPassEvent;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.onekeys.homepage.process.SoftWalletNameSettingActivity;
import org.haobtc.onekey.ui.base.BaseActivity;
import org.haobtc.onekey.ui.dialog.PassInvalidDialog;
import org.haobtc.onekey.utils.PwdEditText;
import org.haobtc.onekey.utils.ViewHeightStatusDetector;
import org.haobtc.onekey.utils.ViewTouchUtil;
import org.haobtc.onekey.viewmodel.PassWordViewModel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * @author liyan
 * @date 12/17/20
 */

public class SoftPassActivity extends BaseActivity implements ViewHeightStatusDetector.VisibilityListener {
    private static final int LONG_PASS_MODE_MIN_HEIGHT = 440;
    private static final int SHORT_PASS_MODE_MIN_HEIGHT = 320;

    public static final int SET = 0;
    public static final int VERIFY = 1;
    public static final int CHANGE = 2;

    private static final String EXT_FROM_TYPE = StringConstant.FROM;
    private static final String EXT_RESULT_FROM_TYPE = "from_type";
    private static final String EXT_RESULT_PWD = "wallet_pwd";

    private static Intent obtainIntent(Context context, int from) {
        Intent intent = new Intent(context, SoftPassActivity.class);
        intent.putExtra(EXT_FROM_TYPE, from);
        return intent;
    }

    public static void start(Context context, int from) {
        context.startActivity(obtainIntent(context, from));
    }

    public static void startForResult(Activity activity, int resultCode, int from) {
        activity.startActivityForResult(obtainIntent(activity.getBaseContext(), from), resultCode);
    }

    public static class ResultDataBean {
        public final String password;
        public final int fromType;

        public ResultDataBean(String password, int fromType) {
            this.password = password;
            this.fromType = fromType;
        }
    }

    public static ResultDataBean decodeResultData(Intent intent) {
        int from = intent.getIntExtra(EXT_RESULT_FROM_TYPE, 44);
        String pwd = intent.getStringExtra(EXT_RESULT_PWD);
        return new ResultDataBean(pwd, from);
    }

    private ViewHeightStatusDetector mViewHeightStatusDetector;
    private ConstraintSet mConstraintSetTipShow, mConstraintSetTipHidden;

    @BindView(R.id.layoutTip)
    ConstraintLayout layoutTip;
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.text_page_title)
    TextView textPageTitle;
    @BindView(R.id.text_pass_promote)
    TextView textPassPromote;
    @BindView(R.id.text_tip)
    TextView textTip;
    @BindView(R.id.edit_pass_short)
    PwdEditText editPassShort;
    @BindView(R.id.edit_pass_long)
    EditText editPassLong;
    @BindView(R.id.img_eye_yes)
    ImageView imgEyeYes;
    @BindView(R.id.img_eye_no)
    ImageView imgEyeNo;
    @BindView(R.id.edit_pass_long_layout)
    LinearLayout editPassLongLayout;
    @BindView(R.id.btn_next)
    Button btnNext;
    @BindView(R.id.pass_type_switch)
    RelativeLayout passTypeSwitch;
    @BindView(R.id.promote)
    TextView promote;
    @BindView(R.id.pass_type_switch_promote)
    TextView passTypeSwitchPromote;
    private ScheduledExecutorService scheduledExecutorService;
    private int operationType;
    private boolean isLongPass;
    private String pinInputFirst;
    private String pinOrigin;
    private int fromType = -1;
    private PassWordViewModel mPassViewModel;


    @Override
    public void init() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        viewHeightStatusListener();
        mPassViewModel = new ViewModelProvider(this).get(PassWordViewModel.class);
        judgeStatus();
        keyBroad();
        ViewTouchUtil.expandViewTouchDelegate(passTypeSwitch, 12);
    }

    private void viewHeightStatusListener() {
        mConstraintSetTipShow = new ConstraintSet();
        mConstraintSetTipHidden = new ConstraintSet();
        mConstraintSetTipShow.clone(layoutTip);
        mConstraintSetTipHidden.clone(layoutTip);
        mConstraintSetTipHidden.setVisibility(R.id.text_tip, View.GONE);
        mViewHeightStatusDetector = new ViewHeightStatusDetector(AutoSizeUtils.dp2px(this, SHORT_PASS_MODE_MIN_HEIGHT));
        mViewHeightStatusDetector.register(this)
                .setVisibilityListener(this);
    }

    /***
     * init layout
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.soft_pass_activity;
    }

    /**
     * 渲染初始视图
     */
    private void judgeStatus() {
        // 默认是校验模式，设置模式不需要单独传递
        operationType = getIntent().getIntExtra(Constant.OPERATE_TYPE, 1);
        mPassViewModel.checkIsSetType(operationType);
        if (getIntent().hasExtra(StringConstant.FROM)) {
            fromType = getIntent().getIntExtra(StringConstant.FROM, -1);
        }
        mPassViewModel.passSetType.observe(this, type -> {
            operationType = type;
            switch (operationType) {
                case SET:
                    textPageTitle.setText(R.string.set_you_pass);
                    break;
                case VERIFY:
                    textPassPromote.setText(R.string.input_your_pass);
                    textTip.setText(R.string.dont_tell);
                    break;
                case CHANGE:
                    textPageTitle.setText(R.string.fix_pass);
                    textPassPromote.setText(R.string.input_pass_origin);
                    textTip.setText(R.string.change_pin_warning);
                    break;
            }
        });
        mPassViewModel.isLongType.observe(this, isLong -> {
            isLongPass = isLong;
            if (isLong) {
                showLongPassLayout(true);
            }
        });
    }

    /**
     * 唤起软键盘
     */
    private void keyBroad() {
        if (editPassShort.getVisibility() == View.VISIBLE) {
            editPassShort.setFocusable(true);
            editPassShort.setFocusableInTouchMode(true);
            editPassShort.requestFocus();
            scheduledExecutorService.schedule(() -> {
                        InputMethodManager inputManager =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(editPassShort, 0);
                    },
                    200, TimeUnit.MILLISECONDS);
        } else {
            editPassLong.setFocusable(true);
            editPassLong.setFocusableInTouchMode(true);
            editPassLong.requestFocus();
            scheduledExecutorService.schedule(() -> {
                        InputMethodManager inputManager =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.showSoftInput(editPassLong, 0);
                    },
                    200, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 隐藏软键盘
     */
    private void hideKeyBroad() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private void showLongPassLayout(boolean yes) {
        isLongPass = !isLongPass;
        if (yes) {
            editPassShort.setVisibility(View.GONE);
            editPassLongLayout.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.VISIBLE);
            passTypeSwitchPromote.setText(R.string.use_short_pass);
            editPassLong.setText("");
            mViewHeightStatusDetector.notification(AutoSizeUtils.dp2px(this, LONG_PASS_MODE_MIN_HEIGHT));
        } else {
            editPassShort.setVisibility(View.VISIBLE);
            editPassLongLayout.setVisibility(View.GONE);
            btnNext.setVisibility(View.GONE);
            passTypeSwitchPromote.setText(R.string.use_long_pass);
            editPassShort.clearText();
            mViewHeightStatusDetector.notification(AutoSizeUtils.dp2px(this, SHORT_PASS_MODE_MIN_HEIGHT));
        }
        keyBroad();
    }

    private void dealVerify(String password) {
        PyResponse<Void> response = PyEnv.verifySoftPass(password);
        String errors = response.getErrors();
        if (Strings.isNullOrEmpty(errors)) {
            hideKeyBroad();
            GotPassEvent gotPassEvent = new GotPassEvent(password);
            gotPassEvent.fromType = fromType;
            EventBus.getDefault().post(gotPassEvent);

            setResult(password, fromType);
            finish();
        } else {
            showToast(R.string.pin_origin_invalid);
        }
    }

    /**
     * 短密码框实时监听
     */
    @OnTextChanged(value = R.id.edit_pass_short, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onTextChanged(Editable editable) {
        if (editable.toString().length() == 6) {
            switch (operationType) {
                case SET:
                    dealSetPass(editable.toString(), false);
                    break;
                case VERIFY:
                    dealVerify(editable.toString());
                    break;
                case CHANGE:
                    dealChangePass(editable.toString(), false);
                    break;
            }
        }
    }

    /**
     * 长密码框实时监听
     */
    @OnTextChanged(value = R.id.edit_pass_long, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onTextChangedLong(Editable editable) {
        int passLength = editable.toString().length();
        btnNext.setEnabled(passLength > 0);
    }

    /**
     * 设置密码
     */
    private void dealSetPass(String pass, boolean isLongPass) {
        if (Strings.isNullOrEmpty(pinInputFirst)) {
            // 第一次输入password
            pinInputFirst = pass;
            textPassPromote.setText(R.string.input_pass_again);
            textTip.setText(R.string.dont_tell);
        } else {
            // 第二次输入password
            if (pinInputFirst.equals(pass)) {
                mPassViewModel.setPassLengthType(isLongPass);
                GotPassEvent gotPassEvent = new GotPassEvent(pinInputFirst);
                gotPassEvent.fromType = fromType;
                EventBus.getDefault().post(gotPassEvent);

                setResult(pinInputFirst, fromType);
                finish();
                return;
            } else {
                showToast(R.string.twice_input_is_not_identical);
            }
        }
        if (isLongPass) {
            editPassLong.setText("");
        } else {
            editPassShort.clearText();
        }
    }

    /**
     * 修改密码
     */
    private void dealChangePass(String pass, boolean isLongPass) {
        if (Strings.isNullOrEmpty(pinOrigin)) {
            // 验证原始 password
            pinOrigin = pass;
            PyResponse<Void> response = PyEnv.verifySoftPass(pinOrigin);
            String errors = response.getErrors();
            if (Strings.isNullOrEmpty(errors)) {
                textPassPromote.setText(R.string.input_new_pass);
                textTip.setVisibility(View.GONE);
            } else {
                pinOrigin = "";
                showToast(R.string.pin_origin_invalid);
            }
        } else {
            if (Strings.isNullOrEmpty(pinInputFirst)) {
                // 第一次输入新password
                pinInputFirst = pass;
                textPassPromote.setText(R.string.input_pass_again);
                textTip.setText(R.string.dont_tell);
                keyBroad();
            } else {
                // 第二次输入password
                passTypeSwitch.setVisibility(View.INVISIBLE);
                if (pinInputFirst.equals(pass)) {
                    PyResponse<Void> response = PyEnv.changeSoftPass(pinOrigin, pass);
                    String errors = response.getErrors();
                    if (Strings.isNullOrEmpty(errors)) {
                        hideKeyBroad();
                        mPassViewModel.setPassLengthType(isLongPass);
                        showToast(R.string.pass_change_success);
                    } else {
                        showToast(R.string.pass_change_failed);
                    }
                    finish();
                    return;
                } else {
                    showToast(R.string.twice_input_is_not_identical);
                }
            }
        }
        if (isLongPass) {
            editPassLong.setText("");
        } else {
            editPassShort.clearText();
        }
    }

    @OnClick({R.id.img_back, R.id.img_eye_yes, R.id.img_eye_no, R.id.btn_next, R.id.pass_type_switch})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.img_eye_yes:
                imgEyeYes.setVisibility(View.GONE);
                imgEyeNo.setVisibility(View.VISIBLE);
                editPassLong.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                editPassLong.setSelection(editPassLong.getText().toString().length());
                break;
            case R.id.img_eye_no:
                imgEyeYes.setVisibility(View.VISIBLE);
                imgEyeNo.setVisibility(View.GONE);
                editPassLong.setTransformationMethod(PasswordTransformationMethod.getInstance());
                editPassLong.setSelection(editPassLong.getText().toString().length());
                break;
            case R.id.btn_next:
                int passLength = editPassLong.getText().toString().length();
                if (passLength < 8) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("type", 0);
                    PassInvalidDialog invalidDialog = new PassInvalidDialog();
                    invalidDialog.setArguments(bundle);
                    invalidDialog.show(getSupportFragmentManager(), "");
                } else if (passLength > 34) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("type", 1);
                    PassInvalidDialog invalidDialog = new PassInvalidDialog();
                    invalidDialog.setArguments(bundle);
                    invalidDialog.show(getSupportFragmentManager(), "");
                } else {
                    switch (operationType) {
                        case SET:
                            dealSetPass(editPassLong.getText().toString(), true);
                            break;
                        case VERIFY:
                            dealVerify(editPassLong.getText().toString());
                            break;
                        case CHANGE:
                            dealChangePass(editPassLong.getText().toString(), true);
                            break;
                    }
                }
                keyBroad();
                break;
            case R.id.pass_type_switch:
                showLongPassLayout(!isLongPass);
                break;
        }
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        TransitionManager.beginDelayedTransition(layoutTip);
        if (visible) {
            mConstraintSetTipShow.applyTo(layoutTip);
            if (promote != null) {
                promote.setVisibility(View.VISIBLE);
            }
        } else {
            mConstraintSetTipHidden.applyTo(layoutTip);
            if (promote != null) {
                promote.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewHeightStatusDetector.unregister(this);
    }

    private void setResult(@NotNull String pwd, int fromType) {
        Intent intent = new Intent();
        intent.putExtra(EXT_RESULT_PWD, pwd);
        intent.putExtra(EXT_RESULT_FROM_TYPE, fromType);
        setResult(RESULT_OK, intent);
    }
}
