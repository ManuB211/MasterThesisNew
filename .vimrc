let fmt = '%A\ %#[javac]\ %f:%l:\ %m,'
      \ . '%A\ %#[aapt]\ %f:%l:\ %m,'
      \ . '%-Z\ %#[javac]\ %p^,'
      \ . '%-C%.%#'
execute 'set errorformat=' . fmt
"set makeprg=ant\ &&\ ant\ ChoreographyTest\ &&\ ant\ junitreport
set makeprg=ant
