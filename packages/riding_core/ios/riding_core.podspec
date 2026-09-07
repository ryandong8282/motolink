Pod::Spec.new do |s|
  s.name             = 'riding_core'
  s.version          = '0.1.0'
  s.summary          = 'MotoLink native riding and push-to-talk lifecycle bridge.'
  s.description      = <<-DESC
Native iOS bridge for audio session, background ride location and future PushToTalk/RTC integration.
                       DESC
  s.homepage         = 'https://github.com/ryandong8282/motolink'
  s.license          = { :type => 'MIT' }
  s.author           = { 'MotoLink' => 'dev@motolink.local' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '16.0'
  s.swift_version = '5.0'
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES' }
end
