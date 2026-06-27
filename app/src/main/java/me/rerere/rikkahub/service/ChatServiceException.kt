package me.rerere.rikkahub.service

open class ChatServiceException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : ChatServiceException(message)

class NotFoundException(message: String) : ChatServiceException(message)
