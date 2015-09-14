# aop-cache
 aop-cache is a light weight cache library for android with [rxjava](https://github.com/ReactiveX/RxJava).
 Just add ONE LINE to make cache.
#### Usage
------
 add @Cache to the function that returns Observable<>.
```java
@Cache(memTimeOutMs = 5000, dbTimeOutMs = 10000, logLevel = 1)
private Observable<String> getResult(int i) {
    return Observable.just(i)
            .map(new Func1<Integer, String>() {
                @Override
                public String call(Integer integer) {
                    StringBuffer buffer = new StringBuffer();
                    try {
                        Thread.sleep(2222);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int j = 0; j < 10; j++) {
                        buffer.append(integer);
                    }
                    return buffer.toString();
                }
            });
}
```

 and call this @Cache function as usual.
```java
final Random random = new Random();
findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        int nextInt = random.nextInt(5);
        final long startTime = System.currentTimeMillis();
        getResult(nextInt)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @DebugTrace
                    @Override
                    public void call(String s) {
                        long usingTime = System.currentTimeMillis() - startTime;
                        String msg;
                        msg = s + " using time " + usingTime;
                        tv.append("\n");
                        if (usingTime > 2222) {
                            tv.append(Html.fromHtml("<font color=\"#ff0000\">"+ msg +"</font>")) ;
                        } else {
                            tv.append(msg);
                        }
                        sv.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
    }
}
```
 the result
![1](./Example/src/main/res/mipmap-hdpi/pic.jpg)
![2](./Example/src/main/res/mipmap-hdpi/pic2.png)
#### Add dependency
------
```groovy
apply plugin: 'com.jakewharton.hugo'

buildscript {
    dependencies {
        ...
        classpath 'com.jakewharton.hugo:hugo-plugin:1.2.1'
    }
}

repositories {
    jcenter()
}

dependencies {
    ...
    compile 'com.github.bluzwang:aopcache:0.9.8c'
    compile 'org.aspectj:aspectjrt:1.8.1'
}
```

