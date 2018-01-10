import hudson.model.*

def queue = Hudson.instance.queue
println "Queue contains ${queue.items.length} items"
queue.clear()
println "Queue cleared"