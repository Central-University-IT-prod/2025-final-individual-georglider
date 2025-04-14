package ru.georglider.prod.exceptions.model

class TooManyRequests : AppException(429, "Too many requests. Please try again later!")