##
# Creates a simple HTML string that redirects to 
# `https://developer.gini.net/gini-mobile-android/#{destination_path}/index.html`.
#
def get_redirecting_index_html(documentation_title, destination_path, ui)
  <<~INDEX_HTML
  <!DOCTYPE html>
  <html lang="en">
    <head>
      <meta charset="UTF-8">
      <title>#{documentation_title}</title>
      <meta http-equiv="refresh" content="0; URL=https://developer.gini.net/gini-mobile-android/#{destination_path}/index.html" />
    </head>
    <body>
      Redirecting to the latest stable version...
    </body>
  </html>
  INDEX_HTML
end
