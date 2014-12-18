require 'rubygems'
require 'bunny'

XMLS_FOLDER="/Users/alinp/work/blinkbox/zz.Distribution.Book"

abort("No such folder #{XMLS_FOLDER}!!!") unless File.exist?(XMLS_FOLDER)

t0 = Time.now
puts "Connecting to RabbitMQ..."
connection = Bunny.new
connection.start
channel = connection.create_channel
exchange = channel.fanout("Distribution.Book.Search.Exchange", :auto_delete => false, :durable => true)
puts "Read files from disk..."
xmls = Dir["#{XMLS_FOLDER}/*.xml"].sort_by{|file|
  File.basename(file, ".xml").to_i
}
puts "Finished reading #{xmls.size} files from disk"
puts "Publish V1 messages..."
xmls.each{|file|
  exchange.publish(File.read(file),
                  :content_type => "application/xml",
                  :headers => {'content-type' => "application/xml"})
}
puts "Completed to publish #{xmls.size} files within #{Time.now - t0} seconds."
sleep 3
puts "Done"

