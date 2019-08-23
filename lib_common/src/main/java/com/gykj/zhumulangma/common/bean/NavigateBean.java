package com.gykj.zhumulangma.common.bean;

import me.yokeyword.fragmentation.ISupportFragment;

/**
 * Author: Thomas.
 * Date: 2019/8/16 14:34
 * Email: 1071931588@qq.com
 * Description:
 */
public class NavigateBean {
   public String path;
   public ISupportFragment fragment;

    public NavigateBean(String path, ISupportFragment fragment) {
        this.path = path;
        this.fragment = fragment;
    }
}