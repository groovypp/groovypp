import java.util.regex.Pattern
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

def createTestFile () {
    def file = new File("trade.csv")
    if(!file.exists()) {
        file.withWriter {writer ->
            (1..10000).each { //10000 scenario
                writer.writeLine "$it," + (1..1000).collect {
                    "${Math.random() * 100}"
                }.join(",") //one price for each of 1000 dates
            }
        }
    }
}


double pfe05_dyn(val) {val[49]}

double pfe95_dyn(val) {val[949]}

double eep_dyn(val) {
    res = val.collect {Math.max(it, 0)}.sum();
    !res ? 0 : res / val.size()
}

double een_dyn(val) {
    res = val.collect {Math.min(it, 0)}.sum();
    !res ? 0 : res / val.size()
}

def calculateDynamic () {
    def result = [:]

    long start = System.currentTimeMillis()
    new File("trade.csv").splitEachLine("\\,") {List<String> tokens ->
        def values = tokens[1..-1].collect {it.toDouble()}.sort()
        result[tokens[0]] = [pfe05: pfe05_dyn(values), pfe95: pfe95_dyn(values),
                eep: eep_dyn(values), een: een_dyn(values)]
    }
    println "dynamic: ${System.currentTimeMillis() - start}"
}

def calculateDynamicFastParse () {
    def result = [:]

    long start = System.currentTimeMillis()
    new File("trade.csv").splitEachLine("\\,") {List<String> tokens ->
        def values = tokens[1..-1].collect {parseDouble(it)}.sort()
        result[tokens[0]] = [pfe05: pfe05_dyn(values), pfe95: pfe95_dyn(values),
                eep: eep_dyn(values), een: een_dyn(values)]
    }
    println "dynamic fast: ${System.currentTimeMillis() - start}"
}

@Typed static double parseDouble(String val) {
    def e = val.lastIndexOf('E')
    int power
    if(e != -1) {
        power = Integer.parseInt(val.substring(e+1))
        val = val.substring(0, e)
    }

    def dot = val.indexOf('.')
    double fract
    if(dot != -1) {
        def fpart = val.substring(dot + 1)
        int k
        while(fpart.charAt(k) == '0') {
            power++
            k++
        }
        if(k)
            fpart = fpart.substring(1)
        
        fract = Long.parseLong(fpart) / Math.pow(10d,fpart.length())
        val = val.substring(0,dot)
    }

    def res = Integer.parseInt(val) + fract
    if(power)
        res = res * Math.pow(10,power)
    res
}

@Typed double pfe05_typed(Double[] val) {val[49]}

@Typed double pfe95_typed(Double[] val) {val[949]}

@Typed double eep_typed(Double[] val) {
    double res = 0;
    for (i in val) {res += Math.max(i, 0)}
    res == 0 ? 0 : res / val.size()
}

@Typed double een_typed(Double[] val) {
    double res = 0;
    for (i in val) {res += Math.min(i, 0)}
    res == 0 ? 0 : res / val.size()
}

@Typed def processLine(String line, Map result) {
    @Field Pattern pattern = Pattern.compile("\\,")

    def tokens = pattern.split(line)
    def values = tokens*.parseDouble()
    Arrays.sort(values)
    result[tokens[0]] = [pfe05: pfe05_typed(values), pfe95: pfe95_typed(values), eep: eep_typed(values), een: een_typed(values)]
}

@Typed def calculateStaticConcurrent () {
    ConcurrentHashMap result = [:]

    def start = System.currentTimeMillis()

    def pool = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())
        Semaphore sema = [10*Runtime.runtime.availableProcessors()]
        for(line in new FileReader("trade.csv")) {
            sema.acquire()
            pool.execute {
                processLine(line, result)
                sema.release()
            }
        }
    pool.shutdown()
    pool.awaitTermination(40, TimeUnit.SECONDS)

    println "concurrent: ${System.currentTimeMillis() - start}"
}

@Typed def calculateStatic () {
    def result = [:]

    def start = System.currentTimeMillis()

    for(line in new FileReader("trade.csv")) {
        processLine(line, result)
    }
    println "static: ${System.currentTimeMillis() - start}"
}

createTestFile ()
for(i in 0..<10) {
    calculateDynamic()
    calculateDynamicFastParse()
    calculateStatic()
    calculateStaticConcurrent()
    println ()
}
