load 'util/git.rb'

##
# Extract the project version from the tag.
#
# The tag must have the following format: `<project-id>;<version>`.
#
# Halts and prints an error if the project id is not in the tag.
#
def get_project_version_from_tag(project_id, tag, ui)
  tag = normalize_tag(tag)
  components = tag.split(';')

  if components[0] != project_id
    ui.user_error! "The project id '#{project_id}' is not in the tag: #{tag}"
  end

  if components.size < 2
    ui.user_error! "Missing project version from the tag: #{tag}"
  end

  components[1].strip
end

##
# Extract the example app version of a project from the tag.
#
# The tag must have the following format: `<project-id>;<version>;<example-app-id>;<version>`.
#
# Halts and prints an error if the project id or example app id is not in the tag.
#
def get_example_app_version_from_tag(project_id, example_app_id, tag, ui)
  tag = normalize_tag(tag)
  components = tag.split(';')

  if components[0] != project_id
    ui.user_error! "The project id '#{project_id}' is not in the tag: #{tag}"
  end

  if components[2] != example_app_id
    ui.user_error! "The example app id '#{example_app_id}' is not in the tag: #{tag}"
  end

  if components.size != 4
    ui.user_error! "Missing example app version from the tag: #{tag}"
  end

  components[3].strip
end

def normalize_tag(tag)
  tag.delete_prefix("refs/tags/")
end

##
# Retrieve the version from the project's "gradle.properties" file.
#
def get_project_version_from_gradle(project_id, module_id)
  gradle_command = "./gradlew #{project_id}:#{module_id}:printVersion -q"
  # Go to repo root, if the current working dir is in the 'fastlane' folder
  if Dir.pwd.include? "fastlane"
    output = sh("cd .. && #{gradle_command}")
  else
    output = sh("#{gradle_command}")
  end
  output.lines.last.strip
end

##
# Retrieve the project's version from its latest release tag.
#
def get_latest_version_from_release_tags(project_id, ui)
  latest_version_tag = get_latest_release_tag(project_id)
  get_project_version_from_tag(project_id, latest_version_tag, ui)
end

##
# Check if the project has a release tag for the given version.
#
def has_release_tag?(project_id, project_version, ui)
  release_tags = get_release_tags(project_id)
  release_tags.any? { |release_tag| 
    get_project_version_from_tag(project_id, release_tag, ui) == project_version
  }
end

##
# Checks if the project version is stable,  meaning that the release version has no suffix (e.g., `1.10.23` is stable and 
# `1.11.0-beta01` is not).
#
def is_project_version_stable?(project_version, ui)
  /^\d+\.\d+\.\d+$/.match(project_version)
end
