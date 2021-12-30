package com.github.douwe.bikenolight.rest;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RouteResource {

    @POST("route/start")
    Call<Long> routeStart(@Body RouteStart routeStart);

    @POST("route/data")
    Call<Void> routeData(@Body RouteData data);
}
