package nu.westlin.aopdemo

import kotlinx.coroutines.delay
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@SpringBootApplication
class AopdemoApplication

fun main(args: Array<String>) {
    runApplication<AopdemoApplication>(*args)
}

@RestController
class FooController(
    private val fooService: FooService
) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("/nottimed")
    suspend fun notTimed(): String {
        delay(431)
        return "notTimed function"
    }

    @GetMapping("/regular")
    @Timed
    fun regular(): String {
        Thread.sleep(431)
        return "regular function"
    }

    @GetMapping("/suspending")
    @Timed
    suspend fun suspending(): String {
        delay(431)
        return "suspending function"
    }

    @GetMapping("/internalfunctioncall")
    @Timed
    suspend fun internalFunctionCall(): String {
        delay(431)
        foo()
        return "internalfunctioncall"
    }

    @Timed
    private suspend fun foo() {
        delay(431)
    }

    @GetMapping("/externalfunctioncall")
    @Timed
    suspend fun externalFunctionCall(): String {
        delay(431)
        fooService.foo()
        return "externalfunctioncall"
    }
}

@Service
class FooService {

    @Timed
    suspend fun foo() {
        delay(431)
    }
}

annotation class Timed

/*
@Aspect
@Component
class LoggingAspect {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @OptIn(ExperimentalTime::class)
    @Around("@annotation(nu.westlin.aopdemo.Timed)")
    @Throws(Throwable::class)
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        return measureTimedValue {
            joinPoint.proceed()
        }.let {
            logger.info("Exekvering av ${joinPoint.signature} tog ${it.duration}")
            it.value
        }
    }
}
*/

/*
@Aspect
@Component
class LoggingAspect {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalTime::class)
    @Around("@annotation(nu.westlin.aopdemo.Timed)")
    @Throws(Throwable::class)
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        // TODO petves: Skriv om till två funktioner, en för suspend och en för "vanliga"

        return if (joinPoint.args.isNotEmpty() && joinPoint.args.last() is Continuation<*>) {
            val continuationParameter = joinPoint.args.last() as Continuation<Any?>
            val otherArgs = joinPoint.args.sliceArray(0 until joinPoint.args.size - 1)

            return runCoroutine(continuationParameter) {
                measureTimedValue {
                    suspendCoroutineUninterceptedOrReturn<Any?> { joinPoint.proceed(otherArgs + it) }
                }.let {
                    logger.info("Exekvering av ${joinPoint.signature} tog ${it.duration}")
                    it.value
                }
            }
        } else {
            measureTimedValue {
                joinPoint.proceed()
            }.let {
                logger.info("Exekvering av ${joinPoint.signature} tog ${it.duration}")
                it.value
            }
        }
    }

    private fun runCoroutine(continuationParameter: Continuation<Any?>, block: suspend () -> Any?): Any? =
        block.startCoroutineUninterceptedOrReturn(continuationParameter)

}
*/

@OptIn(ExperimentalTime::class)
@Aspect
@Component
class LoggingAspect {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Around("@annotation(nu.westlin.aopdemo.Timed) && !args(.., kotlin.coroutines.Continuation)")
    fun logExecutionTimeInNotSuspensionFunction(joinPoint: ProceedingJoinPoint): Any? {
        return measureTimedValue {
            joinPoint.proceed()
        }.let {
            logger.info("Exekvering av ${joinPoint.signature} tog ${it.duration}")
            it.value
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Around("@annotation(nu.westlin.aopdemo.Timed) && args(.., kotlin.coroutines.Continuation)")
    fun logExecutionTimeInSuspensionFunction(joinPoint: ProceedingJoinPoint): Any? {
        return runCoroutine(joinPoint.args.last() as Continuation<Any?>) {
            measureTimedValue {
                suspendCoroutineUninterceptedOrReturn<Any?> { joinPoint.proceed(joinPoint.args.sliceArray(0 until joinPoint.args.size - 1) + it) }
            }.let {
                logger.info("Exekvering av ${joinPoint.signature} tog ${it.duration}")
                it.value
            }
        }
    }

    private fun runCoroutine(continuationParameter: Continuation<Any?>, block: suspend () -> Any?): Any? =
        block.startCoroutineUninterceptedOrReturn(continuationParameter)

}

/*
@Aspect
@Component
class CoroutineTimedAspect {

    @Around("@annotation(ilia.isakhin.timed.coroutines.aspect.SuspendTimed) && args(.., kotlin.coroutines.Continuation)")
    fun logResult(joinPoint: ProceedingJoinPoint): Any? {
        @Suppress("UNCHECKED_CAST")
        val continuationParameter = joinPoint.args.last() as Continuation<Any?>
        val otherArgs = joinPoint.args.sliceArray(0 until joinPoint.args.size - 1)

        return runCoroutine(continuationParameter) {
            //val timer = Timer.start(meterRegistry)

            try {
                suspendCoroutineUninterceptedOrReturn { joinPoint.proceed(otherArgs + it) }
            } finally {
*/
/*
                timer.stop(
                    Timer.builder("my-suspend-metric")
                        .register(meterRegistry)
                )
*//*

            }
        }
    }

    fun runCoroutine(continuationParameter: Continuation<Any?>, block: suspend () -> Any?): Any? =
        block.startCoroutineUninterceptedOrReturn(continuationParameter)
}
*/
