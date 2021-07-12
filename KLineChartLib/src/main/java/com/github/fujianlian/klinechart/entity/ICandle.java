package com.github.fujianlian.klinechart.entity;

/**
 * Candlestick entity interface
 * Created by tifezh on 2016/6/9.
 */
public interface ICandle {

    /**
     * Opening price
     */
    float getOpenPrice();

    /**
     * Highest price
     */
    float getHighPrice();

    /**
     * Lowest price
     */
    float getLowPrice();

    /**
     * Closing price
     */
    float getClosePrice();


    // The following is the MA data
    /**
     * Five (month, day, hour, minute, 5 minutes, etc.) average price
     */
    float getMA5Price();

    /**
     * Ten (month, day, hour, minute, 5 minutes, etc.) average price
     */
    float getMA10Price();

    /**
     * Twenty (month, day, hour, minute, 5 minutes, etc.) average price
     */
    float getMA20Price();

    /**
     * 30 (month, day, hour, minute, 5 minutes, etc.) average price
     */
    float getMA30Price();

    /**
     * Sixty (month, day, hour, minute, 5 minutes, etc.) average price
     */
    float getMA60Price();

    // The following is the BOLL data
    /**
     * Upper rail line
     */
    float getUp();

    /**
     * Middle track
     */
    float getMb();

    /**
     * Lower track
     */
    float getDn();

}
