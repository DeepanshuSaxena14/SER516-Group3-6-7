import shell from "shelljs";

export const execWithTimeout = (command, timeoutMs) => {
  return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
          reject(new Error(`Command timed out after ${timeoutMs / 1000}s: ${command}`));
      }, timeoutMs);

      try {
        shell.exec(command, { silent: true, async: true }, (code, stdout, stderr) => {
          clearTimeout(timer);
          resolve({ code, stdout, stderr });
        });
      }
      catch (error) {
        console.log(error)
      }
  });
};

