##
# Configure git on CI machines.
# 
# Usually CI machines start with a "clean slate" and we need to set
# some configurations before being able to push from the CI machine.
#
def configure_git_on_ci_machines(user_name, user_email) 
  sh("git config --global user.email '#{user_email}'")
  sh("git config --global user.name '#{user_name}'")
end

##
# Pushes the branch to origin. 
# 
# If it fails due to being out-of-date it will do a pull and
# will retry the push.
#
def git_push_with_retry(branch, ui)
  sh("git push origin #{branch}") do |status, result, command|
    if status.success? == false 
      if result.include?("fetch first")
        ui.message "Pulling changes from remote and retrying the push"
        sh("git pull")
        sh(command)
      else
        ui.abort_with_message! "Push failed: #{result}"
      end 
    end
  end
end

##
# Creates a release tag with this format: `<project-id>;<version>`.
#
def git_create_release_tag(project_id, version)
  sh("git tag -a '#{project_id};#{version}' -m '#{project_id};#{version}'")
end
