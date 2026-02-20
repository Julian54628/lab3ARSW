package edu.eci.arsw.blueprints.controllers;

public record ApiResponse<T>(int code, String message, T data) {}
