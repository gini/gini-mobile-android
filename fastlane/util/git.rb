require 'shellwords'
require 'securerandom'

##
# Publishes the local HEAD commit to `branch` as a commit that GitHub creates and signs.
# Retries with a rebase if the branch moved meanwhile (e.g. two docs jobs at once).
#
# Repo is in `owner/name` form (e.g. "gini/gini-mobile-android"), not a URL.
#
def push_as_signed_commit(repo, branch, ui, attempts: 3)
  attempts.times do |attempt|
    message = sh("git log -1 --format=%B", log: false).strip
    tree = sh("git rev-parse 'HEAD^{tree}'", log: false).strip
    parent = sh("git rev-parse HEAD~1", log: false).strip
    # Random suffix (not a timestamp): two CI jobs starting in the same
    # second must not collide on the temp ref — a losing `git push` would
    # fail before entering the `begin...ensure` block, defeating the
    # retry/cleanup contract.
    tmp_branch = "ci-signing-tmp-#{SecureRandom.hex(6)}"

    # Upload the files so GitHub can build the commit from them.
    sh("git push origin HEAD:refs/heads/#{tmp_branch}")
    begin
      sha = sh("gh api repos/#{repo}/git/commits -f message=#{message.shellescape} " \
               "-f tree=#{tree} -f 'parents[]=#{parent}' --jq .sha").strip
      sh("gh api -X PATCH repos/#{repo}/git/refs/heads/#{branch} -f sha=#{sha} -F force=false")
      return sha
    rescue => e
      raise if attempt == attempts - 1
      # Only non-fast-forward failures on the PATCH call are rebase-and-retry
      # territory; re-raise auth, permission, validation, and rate-limit
      # failures immediately so the real error reaches the lane boundary
      # instead of being masked by the retry loop.
      raise unless e.message.match?(/not a fast forward|is at .+ but expected/i)
      ui.message "#{branch} moved, rebasing and retrying: #{e.message}"
      sh("git pull --rebase origin #{branch}")
    ensure
      sh("gh api -X DELETE repos/#{repo}/git/refs/heads/#{tmp_branch} || true")
    end
  end
end

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
  git_create_tag(get_release_tag_name(project_id, version))
end

##
# Returns a release tag name with this format: `<project-id>;<version>`.
#
def get_release_tag_name(project_id, version)
  "#{project_id};#{version}"
end

##
# Creates an annotated tag.
#
def git_create_tag(tag)
  sh("git tag -a '#{tag}' -m '#{tag}'")
end

##
# Pushes the release tag of this format: `<project-id>;<version>`.
#
def git_push_release_tag(project_id, version)
  git_push_tag(get_release_tag_name(project_id, version))
end

##
# Pushes a tag.
#
def git_push_tag(tag)
  sh("git push origin '#{tag}'")
end

##
# Retrieve the latest release tag for the project id.
#
def get_latest_release_tag(project_id)
  sh("git tag --list '#{project_id};*' --sort=taggerdate", log: false).split.last
end

##
# Retrieve the latest release tag for the project id and version.
#
# The returned release tag can also be a documentation release tag.
#
def get_latest_release_tag_for_version(project_id, version)
  release_tag_name = get_release_tag_name(project_id, version)
  sh("git tag --list '#{release_tag_name}*' --sort=taggerdate", log: false).split.last
end

##
# Retrieve all release tags for the project id.
#
def get_release_tags(project_id)
  sh("git tag --list '#{project_id};*'", log: false).split
end

##
# Returns `true` if the folder contains changes since the given release tag of the project.
#
def did_folder_change_since_release_tag(release_tag, folder, ui)
  sh("git diff --quiet HEAD '#{release_tag}' #{folder}", log: false) do |status, result|
      case status.exitstatus
      when 0
        false
      when 1
        true
      else
        ui.abort_with_message! "Failed to check if folder changed: #{result}"
      end
    end
end
