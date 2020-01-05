// IMyAidlBinderStory.aidl
package com.xxun.watch.storydownloadservice;

// Declare any non-default types here with import statements

interface IMyAidlBinderStory {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
     boolean startService(String broadcast);
}
