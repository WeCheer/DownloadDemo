package com.wyc.download.util;

import android.util.ArrayMap;
import android.util.Log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

/**
 * 作者： wyc
 * <p>
 * 创建时间： 2019/4/4 15:03
 * <p>
 * 文件名字： com.wyc.rx
 * <p>
 * 类的介绍：
 */
public class RxBus {
    private final static String TAG = RxBus.class.getSimpleName();
    private static volatile RxBus instance;
    private FlowableProcessor<Object> mFlowableProcessor;
    private ArrayMap<Object, List<Disposable>> mDisposableArray;
    private CompositeDisposable mCompositeDisposable;

    /**
     * 禁止实例化
     */
    private RxBus() {
        mFlowableProcessor = PublishProcessor.create().toSerialized();
        mDisposableArray = new ArrayMap<>();
        mCompositeDisposable = new CompositeDisposable();
    }

    /**
     * 单例模式，暴露调用者给外部
     *
     * @return 当前对象
     */
    public static RxBus getInstance() {
        if (instance == null) {
            synchronized (RxBus.class) {
                if (instance == null) {
                    instance = new RxBus();
                }
            }
        }
        return instance;
    }

    /**
     * 注册RxBus
     *
     * @param observable 注册对象
     */
    public synchronized void register(final Object observable) {
        //标记是否注册成功
        boolean isRegisterSuccess = false;
        //获取注册类中所有方法
        Method[] methods = observable.getClass().getMethods();
        //判断当前注册类是否已注册
        if (mDisposableArray.containsKey(observable)) {
            Log.d(TAG, "observable has already register !");
            return;
        }
        for (Method method : methods) {
            //判断方法注解类是否是Subscribe
            if (!method.isAnnotationPresent(Subscribe.class)) {
                continue;
            }
            //获取注解类
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            final Method subscriptionMethod = method;
            //获取方法中第一个参数类型
            Class<?> key = subscriptionMethod.getParameterTypes()[0];
            //背压处理
            Disposable disposable = mFlowableProcessor.ofType(key)
                    .observeOn(getScheduler(annotation.scheduler()))
                    .subscribe(new Consumer<Object>() {
                        @Override
                        public void accept(Object o) {
                            try {
                                //使用反射设置数据
                                subscriptionMethod.setAccessible(true);
                                subscriptionMethod.invoke(observable, o);
                            } catch (Exception e) {
                                throw new RuntimeException(e.getCause());
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            Log.e(TAG, "register [onError] : ", throwable);
                        }
                    });
            mCompositeDisposable.add(disposable);
            List<Disposable> disposableList;
            //获取注册类获取类中的所有注解为Subscribe的方法，并保存
            if (mDisposableArray.containsKey(observable)) {
                disposableList = mDisposableArray.get(observable);
            } else {
                disposableList = new ArrayList<>();
            }
            disposableList.add(disposable);
            //保存数据，反注册是使用
            mDisposableArray.put(observable, disposableList);
            //注册成功
            isRegisterSuccess = true;
        }
        if (!isRegisterSuccess) {
            //没有注册成功，抛出自定义异常
            throw new RuntimeException(observable.getClass().getName() + " has no any RxBuxSubscribe Event!");
        }
    }

    /**
     * 反注册RxBus
     *
     * @param observable 反注册对象
     */
    public synchronized void unregister(Object observable) {
        if (!mDisposableArray.containsKey(observable)) return;
        List<Disposable> disposableList = mDisposableArray.get(observable);
        for (Disposable disposable : disposableList) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }
        mCompositeDisposable.dispose();
        mDisposableArray.remove(observable);
    }

    /**
     * 发送数据
     *
     * @param event 发送事件
     */
    public void post(Object event) {
        if (mFlowableProcessor.hasSubscribers()) {
            mFlowableProcessor.onNext(event);
        }
    }

    /**
     * 自定义处理
     *
     * @param eventType 事件类型
     * @param <T>       泛型
     * @return 返回Flowable泛型
     */
    public <T> Flowable<T> getObservable(Class<T> eventType) {
        return mFlowableProcessor.ofType(eventType);
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Subscribe {
        RunningThreadType scheduler() default RunningThreadType.io;
    }

    public enum RunningThreadType {
        single, trampoline, newThread, computation, io, mainThread
    }

    private Scheduler getScheduler(RunningThreadType type) {
        switch (type) {
            case single:
                return Schedulers.single();
            case trampoline:
                return Schedulers.trampoline();
            case newThread:
                return Schedulers.newThread();
            case computation:
                return Schedulers.computation();
            case io:
                return Schedulers.io();
            case mainThread:
                return AndroidSchedulers.mainThread();
            default:
                return Schedulers.io();
        }
    }
}
