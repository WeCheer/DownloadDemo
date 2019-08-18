package com.wyc.download.event;

/**
 * 作者： wyc
 * <p>
 * 创建时间： 2019/7/16 9:08
 * <p>
 * 文件名字： com.wyc.download.event
 * <p>
 * 类的介绍：RxBus总线
 */
public class ProgressEvent {
    private int progress;
    private boolean isCompleted;
    private String errorMsg;

    public ProgressEvent(int progress) {
        this.progress = progress;
    }

    public ProgressEvent(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public ProgressEvent(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

}
