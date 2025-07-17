package com.example.blescanner.koinlesson

class ProdApiClient : IApiClient {}

class TestApiClient : IApiClient {}

interface IApiClient {}

class ApiClientFactory(private val apiClient: IApiClient) {}