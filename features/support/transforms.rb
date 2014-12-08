
CAPTURE_INTEGER = Transform(/^(?:-?\d+|one|two|three|four|five|six|seven|eight|nine|ten)$/) do |value|
  case value
  when "one" then 1
  when "two" then 2
  when "three" then 3
  when "four" then 4
  when "five" then 5
  when "six" then 6
  when "seven" then 7
  when "eight" then 8
  when "nine" then 9
  when "ten" then 10
  else value.to_i
  end
end

CAPTURE_NAMED_INDEX = Transform(/^first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth$/) do |value|
  case value
  when "first" then 0
  when "second" then 1
  when "third" then 2
  when "fourth" then 3
  when "fifth" then 4
  when "sixth" then 5
  when "seventh" then 6
  when "eighth" then 7
  when "ninth" then 8
  when "tenth" then 9
  end
end