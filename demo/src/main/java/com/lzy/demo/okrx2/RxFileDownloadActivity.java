/*
 * Copyright 2016 jeasonlzy(廖子尧)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lzy.demo.okrx2;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lzy.demo.R;
import com.lzy.demo.base.BaseRxDetailActivity;
import com.lzy.demo.ui.NumberProgressBar;
import com.lzy.demo.utils.Urls;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.convert.FileConvert;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okrx.adapter.ObservableResponse;

import java.io.File;
import java.text.NumberFormat;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * ================================================
 * 作    者：jeasonlzy（廖子尧）Github地址：https://github.com/jeasonlzy
 * 版    本：1.0
 * 创建日期：16/9/11
 * 描    述：
 * 修订历史：
 * ================================================
 */
public class RxFileDownloadActivity extends BaseRxDetailActivity {

    @Bind(R.id.et_url) EditText etUrl;
    @Bind(R.id.fileDownload1) Button btnFileDownload1;
    @Bind(R.id.fileDownload2) Button btnFileDownload2;
    @Bind(R.id.downloadSize) TextView tvDownloadSize;
    @Bind(R.id.tvProgress) TextView tvProgress;
    @Bind(R.id.netSpeed) TextView tvNetSpeed;
    @Bind(R.id.pbProgress) NumberProgressBar pbProgress;
    private NumberFormat numberFormat;

    @Override
    protected void onActivityCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_rx_file_download);
        ButterKnife.bind(this);
        setTitle("文件下载");

        numberFormat = NumberFormat.getPercentInstance();
        numberFormat.setMinimumFractionDigits(2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Activity销毁时，取消网络请求
        unSubscribe();
    }

    @OnClick(R.id.fileDownload1)
    public void fileDownload1(View view) {
        //使用okrx直接下，下载进度封装比较麻烦,推荐使用回调方式
        String etString = etUrl.getText().toString();
        String url = TextUtils.isEmpty(etString) ? Urls.URL_DOWNLOAD : etString;
        OkGo.<File>get(url)//
                .headers("aaa", "111")//
                .params("bbb", "222")//
                .converter(new FileConvert())//
                .adapt(new ObservableResponse<File>()).subscribeOn(Schedulers.io())//
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        btnFileDownload1.setText("正在下载中...\n使用Rx方式做进度监听稍显麻烦,推荐使用方式2");
                    }
                })//
                .observeOn(AndroidSchedulers.mainThread())//
                .subscribe(new Subscriber<Response<File>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        btnFileDownload1.setText("下载出错");
                        showToast(e.getMessage());
                        handleError(null);
                    }

                    @Override
                    public void onNext(Response<File> response) {
                        btnFileDownload1.setText("下载完成");
                        handleResponse(response);
                    }
                });
    }

    @OnClick(R.id.fileDownload2)
    public void fileDownload2(View view) {
        Observable.create(new Observable.OnSubscribe<Progress>() {
            @Override
            public void call(final Subscriber<? super Progress> subscriber) {
                String etString = etUrl.getText().toString();
                String url = TextUtils.isEmpty(etString) ? Urls.URL_DOWNLOAD : etString;
                OkGo.<File>get(url)//
                        .headers("aaa", "111")//
                        .params("bbb", "222")//
                        .execute(new FileCallback() {
                            @Override
                            public void onSuccess(Response<File> response) {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Response<File> response) {
                                subscriber.onError(response.getException());
                            }

                            @Override
                            public void downloadProgress(Progress progress) {
                                subscriber.onNext(progress);
                            }
                        });
            }
        })//
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        btnFileDownload2.setText("正在下载中...");
                    }
                })//
                .observeOn(AndroidSchedulers.mainThread())//
                .subscribe(new Subscriber<Progress>() {
                    @Override
                    public void onCompleted() {
                        btnFileDownload2.setText("下载完成");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        btnFileDownload2.setText("下载出错");
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onNext(Progress progress) {
                        String downloadLength = Formatter.formatFileSize(getApplicationContext(), progress.currentSize);
                        String totalLength = Formatter.formatFileSize(getApplicationContext(), progress.totalSize);
                        tvDownloadSize.setText(downloadLength + "/" + totalLength);
                        String speed = Formatter.formatFileSize(getApplicationContext(), progress.speed);
                        tvNetSpeed.setText(String.format("%s/s", speed));
                        tvProgress.setText(numberFormat.format(progress.fraction));
                        pbProgress.setMax(10000);
                        pbProgress.setProgress((int) (progress.fraction * 10000));
                    }
                });
    }
}
