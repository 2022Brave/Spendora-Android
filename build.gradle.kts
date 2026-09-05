tasks.register<Exec>("assembleDebug") {
    commandLine("npm", "run", "build")
}

tasks.register<Exec>("build") {
    commandLine("npm", "run", "build")
}

tasks.register<Exec>("check") {
    commandLine("npm", "run", "build")
}
