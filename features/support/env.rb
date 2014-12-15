$: << __dir__

require "httpclient/capture"
require "cucumber/rest"
require "cucumber/helpers"
require "cucumber/blinkbox"

TEST_CONFIG["server"] = ENV["SERVER"] || "local"
TEST_CONFIG["proxy"] = ENV["PROXY_SERVER"]
TEST_CONFIG["debug"] = !!(ENV["DEBUG"] =~ /^on|true$/i)
TEST_CONFIG["fail_fast"] = !!(ENV["FAIL_FAST"] =~ /^on|true$/i)
TEST_CONFIG["stats"] = !!(ENV["STATS"] =~ /^on|true$/i)

puts "TEST_CONFIG: #{TEST_CONFIG}" if TEST_CONFIG["debug"]

# ======= statistics =======
if TEST_CONFIG["stats"]
  def colorize(text, color_code)
    "\e[#{color_code}m#{text}\e[0m"
  end
  def red(text); colorize(text, 31); end
  def green(text); colorize(text, 32); end
  def yellow(text); colorize(text, 33); end

  def print_stats(group_by = :path)
    responses = HttpCapture::RESPONSES.keep_if { |response| response.successful? }
    stats = responses.map { |response| 
                            { 
                              endpoint: "#{response.request.method} #{response.request.uri.send(group_by)}", 
                              duration: response.duration 
                            } 
                          }.group_by { |call| 
                            call[:endpoint] 
                          }.map { |endpoint, calls| 
                            avg_duration = calls.sum { |call| call[:duration] } / calls.count
                            {
                              endpoint: endpoint, 
                              duration: (avg_duration * 1000).to_i
                            }
                          }.sort { |a, b|
                            b[:duration] <=> a[:duration]
                          }
    stats.each do |stat|
      color = if stat[:duration] > 250 then :red
              elsif stat[:duration] > 100 then :yellow
              else :green
              end
      puts send(color, "#{stat[:duration].to_s.rjust(5)}ms  =>  #{stat[:endpoint]}")
    end
  end

  at_exit do
    puts
    puts "Statistics by path:"
    print_stats
    puts
    puts "Statistics by path and query:"
    print_stats(:request_uri)
    puts
  end
end
